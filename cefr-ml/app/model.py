"""
ONNX CEFR classifier with chunked prediction and a feature-factor breakdown.
"""

import gzip
import json
import re
from pathlib import Path

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

from features import extract_features

LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]
MODEL_DIR = Path(__file__).resolve().parent / "model"
MAX_TOKENS = 384

SENTENCE_SPLIT = re.compile(r"(?<=[.!?…])\s+")


class CefrClassifier:
    """
    Loads the ONNX model, tokenizer, and feature probe once.
    """

    def __init__(self, model_dir=MODEL_DIR):
        self.model_dir = Path(model_dir)
        self.session = ort.InferenceSession(
            str(self.model_dir / "model.onnx"), providers=["CPUExecutionProvider"]
        )
        self.tokenizer = Tokenizer.from_file(str(self.model_dir / "tokenizer.json"))
        self.pad_id = self.tokenizer.token_to_id("[PAD]")
        probe = json.loads((self.model_dir / "features.json").read_text(encoding="utf-8"))
        with gzip.open(self.model_dir / "greeklex.json.gz", "rt", encoding="utf-8") as f:
            self.frequency = json.load(f)
        self.feature_names = probe["names"]
        self.means = probe["means"]
        self.stds = probe["stds"]
        self.association = probe["association"]
        self.weight = probe["weight"]
        self.coef = np.array(probe["coef"], dtype=np.float32)
        self.intercept = np.array(probe["intercept"], dtype=np.float32)
        # BERT only ever sharpens, never flattens overconfident outputs.
        self.temperature = min(float(probe.get("temperature", 1.0)), 1.0)
        # Tiny fragments are underdetermined, so nudge them toward the low-mid range
        # rather than letting the model pick a near-random high level.
        self.tiny_words = 30
        self.tiny_prior = np.array([0.2, 0.3, 0.3, 0.15, 0.03, 0.02], dtype=np.float32)
        self.input_names = [i.name for i in self.session.get_inputs()]

    def _bert_weight(self, word_count: int, rare_ratio: float) -> float:
        """
        Returns how much the BERT contributes for a given text.

        The feature probe is more reliable than the BERT on short inputs and on
        long rare-vocabulary text, so it gains weight in those cases.
        """
        if word_count <= 50:
            return 0.4
        if word_count <= 120:
            return 0.6
        if rare_ratio > 0.4:
            return 0.6
        return 0.85

    def _chunks(self, text: str):
        """
        Splits text into sentence-packed chunks that fit the token limit.
        """
        sentences = [s.strip() for s in SENTENCE_SPLIT.split(text) if s.strip()]
        chunks, current = [], []
        for sentence in sentences:
            candidate = current + [sentence]
            token_count = len(self.tokenizer.encode(" ".join(candidate)).ids)
            if token_count + 2 > MAX_TOKENS and current:
                chunks.append(" ".join(current))
                current = [sentence]
            else:
                current = candidate
        if current:
            chunks.append(" ".join(current))
        return chunks or [text]

    def classify(self, text: str):
        """
        Returns level, confidence, probabilities, and factor breakdown.

        The temperature-calibrated BERT and the feature probe are blended, and
        very short inputs are pulled toward a word-count level prior.
        """
        chunks = self._chunks(text)
        bert_probs = np.zeros(len(LEVELS))
        for chunk in chunks:
            enc = self.tokenizer.encode(chunk, add_special_tokens=True)
            input_ids = np.array([enc.ids], dtype=np.int64)
            attention = np.ones_like(input_ids)
            type_ids = np.zeros_like(input_ids)
            feeds = {
                "input_ids": input_ids,
                "attention_mask": attention,
                "token_type_ids": type_ids,
            }
            if "token_type_ids" not in self.input_names:
                feeds.pop("token_type_ids")
            logits = self.session.run(None, feeds)[0]
            logits = logits / self.temperature
            exp = np.exp(logits[0] - logits[0].max())
            bert_probs += exp / exp.sum()
        bert_probs = bert_probs / bert_probs.sum()

        features = extract_features(text, self.frequency)
        probe_probs = self._probe_probs(features)
        word_count = len(text.split())
        bert_weight = self._bert_weight(word_count, features["rareWordRatio"])
        model_probs = bert_weight * bert_probs + (1 - bert_weight) * probe_probs

        if word_count < self.tiny_words:
            probs = 0.3 * model_probs + 0.7 * self.tiny_prior
        else:
            probs = model_probs
        probs = probs / probs.sum()

        level_idx = int(np.argmax(probs))
        return {
            "level": LEVELS[level_idx],
            "confidence": float(probs[level_idx]),
            "probabilities": [
                {"level": level, "probability": float(p)} for level, p in zip(LEVELS, probs)
            ],
            "factors": self._factors(features),
        }

    def _probe_probs(self, features):
        """
        Predicts class probabilities from the standardized linguistic features.
        """
        vector = np.array([features[name] for name in self.feature_names], dtype=np.float32)
        means = np.array([self.means[name] for name in self.feature_names], dtype=np.float32)
        stds = np.array([self.stds[name] or 1.0 for name in self.feature_names], dtype=np.float32)
        z = (vector - means) / stds
        logits = z @ self.coef.T + self.intercept
        exp = np.exp(logits - logits.max())
        return exp / exp.sum()

    def _factors(self, features):
        """
        Computes the factor values with direction and importance.
        """
        factors = []
        for name, value in features.items():
            mean = self.means[name]
            std = self.stds[name] or 1.0
            z = (value - mean) / std
            association = self.association.get(name, 1)
            direction = "up" if (z > 0) == (association > 0) else "down"
            factors.append({
                "name": name,
                "value": float(value),
                "direction": direction,
                "weight": self.weight.get(name, 0.0),
            })
        factors.sort(key=lambda f: f["weight"], reverse=True)
        return factors
