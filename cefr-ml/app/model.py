"""
ONNX CEFR classifier with chunked prediction and a feature-factor breakdown.
"""

import gzip
import json
import os
import re
from pathlib import Path

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

from features import extract_features
from nvidia import preload_nvidia_libs

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
        self.session = self._build_session()
        self.input_names = [i.name for i in self.session.get_inputs()]
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

    def _build_session(self):
        """
        Builds the ONNX session on GPU, falling back to CPU when unavailable.
        """
        preload_nvidia_libs()
        options = ort.SessionOptions()
        options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        options.intra_op_num_threads = min(16, os.cpu_count() or 1)
        available = set(ort.get_available_providers())
        providers = [p for p in ("CUDAExecutionProvider", "CPUExecutionProvider") if p in available]
        if not providers:
            providers = ["CPUExecutionProvider"]
        try:
            return ort.InferenceSession(
                str(self.model_dir / "model.onnx"),
                sess_options=options,
                providers=providers,
            )
        except Exception:
            return ort.InferenceSession(
                str(self.model_dir / "model.onnx"),
                sess_options=options,
                providers=["CPUExecutionProvider"],
            )

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
        if not sentences:
            return [text]
        lengths = [self._token_length(sentence) for sentence in sentences]
        chunks, current, budget = [], [], 0
        for sentence, length in zip(sentences, lengths):
            if current and budget + length > MAX_TOKENS:
                chunks.append(" ".join(current))
                current, budget = [sentence], length
            else:
                current.append(sentence)
                budget += length
        if current:
            chunks.append(" ".join(current))
        return chunks

    def _token_length(self, text: str) -> int:
        """
        Counts real tokens, ignoring the tokenizer's baked-in padding.
        """
        ids = self.tokenizer.encode(text, add_special_tokens=True).ids
        return len(ids) - self._trailing_pads(ids)

    def _encode(self, texts):
        """
        Encodes texts and strips trailing padding, returning ids and lengths.
        """
        encoded = []
        for text in texts:
            ids = self.tokenizer.encode(text, add_special_tokens=True).ids
            length = len(ids) - self._trailing_pads(ids)
            encoded.append((ids[:length], length))
        return encoded

    def _trailing_pads(self, ids):
        """
        Returns how many padding tokens close out an encoded sequence.
        """
        count = 0
        for token in reversed(ids):
            if token != self.pad_id:
                break
            count += 1
        return count

    def _run_batch(self, encoded):
        """
        Runs one ONNX pass over all chunks, padded to the longest in the batch.
        """
        if not encoded:
            return np.zeros((0, len(LEVELS)), dtype=np.float32)
        max_len = max(length for _, length in encoded)
        input_ids = np.full((len(encoded), max_len), self.pad_id, dtype=np.int64)
        mask = np.zeros((len(encoded), max_len), dtype=np.int64)
        for row, (ids, length) in enumerate(encoded):
            input_ids[row, :length] = ids
            mask[row, :length] = 1
        feeds = {
            "input_ids": input_ids,
            "attention_mask": mask,
            "token_type_ids": np.zeros_like(input_ids),
        }
        if "token_type_ids" not in self.input_names:
            feeds.pop("token_type_ids")
        return self.session.run(None, feeds)[0]

    def _bert_probs(self, chunks):
        """
        Predicts level probabilities, averaged over all chunks.
        """
        if not chunks:
            return np.zeros(len(LEVELS))
        logits = self._run_batch(self._encode(chunks))
        scaled = logits / self.temperature
        exp = np.exp(scaled - scaled.max(axis=1, keepdims=True))
        probs = exp / exp.sum(axis=1, keepdims=True)
        return probs.mean(axis=0)

    def classify(self, text: str):
        """
        Returns level, confidence, probabilities, and factor breakdown.

        The temperature-calibrated BERT and the feature probe are blended, and
        very short inputs are pulled toward a word-count level prior.
        """
        bert_probs = self._bert_probs(self._chunks(text))
        features = extract_features(text, self.frequency)
        probe_probs = self._probe_probs(features)
        word_count = len(text.split())
        bert_weight = self._bert_weight(word_count, features["rareWordRatio"])
        model_probs = bert_weight * bert_probs + (1 - bert_weight) * probe_probs

        if word_count < self.tiny_words:
            model_probs = 0.3 * model_probs + 0.7 * self.tiny_prior
        probs = model_probs / model_probs.sum()

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
