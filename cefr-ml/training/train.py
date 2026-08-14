#!/usr/bin/env python3
"""
Fine-tunes bert-base-greek-uncased for CEFR classification, evaluates it,
exports the model to ONNX, and trains a feature probe for the factor breakdown.
"""

import argparse
import gzip
import json
import math
import random
import sys
from collections import Counter
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, f1_score
from torch.utils.data import DataLoader, Dataset
from transformers import (
    BertForSequenceClassification,
    AutoTokenizer,
    get_linear_schedule_with_warmup,
)

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "app"))
from features import extract_features  # noqa: E402

LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]
LEVEL_INDEX = {level: i for i, level in enumerate(LEVELS)}


class CefrDataset(Dataset):
    """
    Holds tokenized training samples with their CEFR labels.
    """

    def __init__(self, samples, tokenizer, max_len):
        """
        Tokenizes each sample and maps its level to an index.
        """
        self.encodings = []
        self.labels = []
        for sample in samples:
            enc = tokenizer(
                sample["text"],
                truncation=True,
                max_length=max_len,
                padding="max_length",
            )
            self.encodings.append(enc)
            self.labels.append(LEVEL_INDEX[sample["level"]])

    def __len__(self):
        """
        Returns the number of samples.
        """
        return len(self.labels)

    def __getitem__(self, i):
        """
        Returns the tensors for the sample at the given index.
        """
        enc = self.encodings[i]
        return {
            "input_ids": torch.tensor(enc["input_ids"]),
            "attention_mask": torch.tensor(enc["attention_mask"]),
            "token_type_ids": torch.tensor(enc.get("token_type_ids", [0] * len(enc["input_ids"]))),
            "labels": torch.tensor(self.labels[i]),
        }


def load_samples(path):
    """
    Loads the samples from a JSONL file.
    """
    samples = []
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        samples.append(json.loads(line))
    return samples


def evaluate(model, loader, device):
    """
    Predicts on the loader and returns the true and predicted labels.
    """
    model.eval()
    preds, labels = [], []
    with torch.no_grad():
        for batch in loader:
            out = model(
                input_ids=batch["input_ids"].to(device),
                attention_mask=batch["attention_mask"].to(device),
                token_type_ids=batch["token_type_ids"].to(device),
            )
            preds.extend(out.logits.argmax(dim=-1).cpu().tolist())
            labels.extend(batch["labels"].tolist())
    return labels, preds


def train_epoch(model, loader, optimizer, scheduler, criterion, device):
    """
    Runs one training pass over the loader.
    """
    model.train()
    total_loss = 0
    for batch in loader:
        optimizer.zero_grad()
        out = model(
            input_ids=batch["input_ids"].to(device),
            attention_mask=batch["attention_mask"].to(device),
            token_type_ids=batch["token_type_ids"].to(device),
        )
        loss = criterion(out.logits, batch["labels"].to(device))
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        optimizer.step()
        scheduler.step()
        total_loss += loss.item()
    return total_loss / len(loader)


def main():
    """
    Trains the classifier, evaluates it, and writes the ONNX + probe artifacts.
    """
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", default="data/dataset.jsonl")
    parser.add_argument("--out-dir", default="app/model")
    parser.add_argument("--model-name", default="nlpaueb/bert-base-greek-uncased-v1")
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--max-len", type=int, default=384)
    parser.add_argument("--lr", type=float, default=2e-5)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--export-only", action="store_true",
                        help="skip training; export the saved model and rebuild the probe")
    args = parser.parse_args()

    if args.export_only:
        samples = load_samples(args.dataset)
        export_onnx(args.out_dir, args.model_name)
        train_feature_probe(samples, args.out_dir)
        return

    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)

    samples = load_samples(args.dataset)
    train = [s for s in samples if s["split"] == "train"]
    val = [s for s in samples if s["split"] == "val"]
    test = [s for s in samples if s["split"] == "test"]
    print(f"train={len(train)} val={len(val)} test={len(test)}")

    tokenizer = AutoTokenizer.from_pretrained(args.model_name)
    model = BertForSequenceClassification.from_pretrained(args.model_name, num_labels=6)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model.to(device)

    counts = Counter(s["level"] for s in train)
    weights = torch.tensor(
        [1.0 / max(1, counts[l]) for l in LEVELS], dtype=torch.float32
    ).to(device)
    weights = weights / weights.sum() * len(LEVELS)
    criterion = nn.CrossEntropyLoss(weight=weights)

    train_loader = DataLoader(CefrDataset(train, tokenizer, args.max_len), batch_size=args.batch_size, shuffle=True)
    val_loader = DataLoader(CefrDataset(val, tokenizer, args.max_len), batch_size=args.batch_size)
    test_loader = DataLoader(CefrDataset(test, tokenizer, args.max_len), batch_size=args.batch_size)

    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr)
    steps = args.epochs * len(train_loader)
    scheduler = get_linear_schedule_with_warmup(optimizer, int(0.1 * steps), steps)

    best_f1 = 0.0
    for epoch in range(1, args.epochs + 1):
        loss = train_epoch(model, train_loader, optimizer, scheduler, criterion, device)
        labels, preds = evaluate(model, val_loader, device)
        macro = f1_score(labels, preds, average="macro")
        print(f"epoch {epoch}: train_loss={loss:.4f} val_macro_f1={macro:.4f}")
        if macro > best_f1:
            best_f1 = macro
            model.save_pretrained(args.out_dir)
            tokenizer.save_pretrained(args.out_dir)

    model = BertForSequenceClassification.from_pretrained(args.out_dir)
    model.to(device)
    labels, preds = evaluate(model, test_loader, device)
    print("=== test ===")
    print(classification_report(labels, preds, target_names=LEVELS, digits=3))
    print(f"macro_f1={f1_score(labels, preds, average='macro'):.4f}")

    export_onnx(args.out_dir, args.model_name)
    train_feature_probe(samples, args.out_dir)


def export_onnx(model_dir, base_model_name):
    """
    Exports the saved model to an ONNX sequence-classification artifact.
    """
    model = BertForSequenceClassification.from_pretrained(str(model_dir))
    model.eval()
    seq_len = 384
    dummy = (
        torch.ones(1, seq_len, dtype=torch.long),
        torch.ones(1, seq_len, dtype=torch.long),
        torch.zeros(1, seq_len, dtype=torch.long),
    )
    out = Path(model_dir) / "model.onnx"
    print("exporting to ONNX...")
    torch.onnx.export(
        model,
        dummy,
        str(out),
        input_names=["input_ids", "attention_mask", "token_type_ids"],
        output_names=["logits"],
        dynamic_axes={
            "input_ids": {0: "batch", 1: "seq"},
            "attention_mask": {0: "batch", 1: "seq"},
            "token_type_ids": {0: "batch", 1: "seq"},
        },
        opset_version=14,
    )
    print("ONNX exported to", out)


def train_feature_probe(samples, out_dir):
    """
    Trains a multinomial logistic regression on the linguistic features.
    """
    lexicon = Path(out_dir) / "greeklex.json.gz"
    with gzip.open(lexicon, "rt", encoding="utf-8") as f:
        frequency = json.load(f)

    rows, ys = [], []
    for s in samples:
        if s["split"] != "train":
            continue
        rows.append(extract_features(s["text"], frequency))
        ys.append(LEVEL_INDEX[s["level"]])
    X = np.array([[r[k] for k in rows[0]] for r in rows])
    feature_names = list(rows[0].keys())

    means = X.mean(axis=0)
    stds = X.std(axis=0)
    stds[stds == 0] = 1.0
    Xs = (X - means) / stds

    probe = LogisticRegression(max_iter=2000)
    probe.fit(Xs, ys)

    # Per-feature: association sign (higher -> higher level?) and importance.
    coeffs = probe.coef_  # (6, n_features)
    level_trend = np.arange(6)
    associations = {}
    weights = {}
    for i, name in enumerate(feature_names):
        slope = np.dot(coeffs[:, i], level_trend - level_trend.mean()) / max(1e-9, coeffs[:, i].std() * level_trend.std())
        associations[name] = 1 if slope > 0 else -1
        weights[name] = float(np.abs(coeffs[:, i]).mean())
    max_weight = max(weights.values()) or 1.0

    features_json = {
        "names": feature_names,
        "means": {name: float(means[i]) for i, name in enumerate(feature_names)},
        "stds": {name: float(stds[i]) for i, name in enumerate(feature_names)},
        "association": associations,
        "weight": {name: weights[name] / max_weight for name in feature_names},
        "coef": probe.coef_.tolist(),
        "intercept": probe.intercept_.tolist(),
        "blendAlpha": 0.7,
    }
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    (out / "features.json").write_text(
        json.dumps(features_json, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("feature probe written to", out / "features.json")


if __name__ == "__main__":
    main()
