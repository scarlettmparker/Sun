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
        self.text_ids = []
        for sample in samples:
            enc = tokenizer(
                sample["text"],
                truncation=True,
                max_length=max_len,
                padding=False,
            )
            self.encodings.append({
                "input_ids": torch.tensor(enc["input_ids"]),
                "attention_mask": torch.tensor(enc["attention_mask"]),
                "token_type_ids": torch.tensor(enc.get("token_type_ids", [0] * len(enc["input_ids"]))),
            })
            self.labels.append(LEVEL_INDEX[sample["level"]])
            self.text_ids.append(sample["textId"])

    def __len__(self):
        """
        Returns the number of samples.
        """
        return len(self.labels)

    def __getitem__(self, i):
        """
        Returns the tensors for the sample at the given index.
        """
        return {
            **self.encodings[i],
            "labels": torch.tensor(self.labels[i]),
            "text_ids": self.text_ids[i],
        }


def collate_batch(batch):
    """
    Pads a batch to its longest sample.
    """
    input_ids = [b["input_ids"] for b in batch]
    labels = torch.stack([b["labels"] for b in batch])
    text_ids = [b["text_ids"] for b in batch]
    max_len = max(len(ids) for ids in input_ids)
    padded = torch.zeros((len(batch), max_len), dtype=torch.long)
    mask = torch.zeros_like(padded)
    for i, ids in enumerate(input_ids):
        padded[i, : len(ids)] = ids
        mask[i, : len(ids)] = 1
    return {
        "input_ids": padded,
        "attention_mask": mask,
        "token_type_ids": torch.zeros_like(padded),
        "labels": labels,
        "text_ids": text_ids,
    }


def make_loader(dataset, batch_size, shuffle, device):
    """
    Builds a dataloader with dynamic padding and pinned transfers.
    """
    return DataLoader(
        dataset,
        batch_size=batch_size,
        shuffle=shuffle,
        collate_fn=collate_batch,
        pin_memory=device.type == "cuda",
    )


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
    Predicts on the loader and returns true labels, predictions, and text ids.
    """
    model.eval()
    preds, labels, text_ids = [], [], []
    with torch.no_grad():
        for batch in loader:
            out = model(
                input_ids=batch["input_ids"].to(device),
                attention_mask=batch["attention_mask"].to(device),
                token_type_ids=batch["token_type_ids"].to(device),
            )
            preds.extend(out.logits.argmax(dim=-1).cpu().tolist())
            labels.extend(batch["labels"].tolist())
            text_ids.extend(batch["text_ids"])
    return labels, preds, text_ids


def text_level(labels, preds, text_ids):
    """
    Majority-votes per-chunk predictions by text and returns the text labels.
    """
    votes = {}
    for label, pred, text_id in zip(labels, preds, text_ids):
        entry = votes.setdefault(text_id, [[0] * len(LEVELS), [0] * len(LEVELS)])
        entry[0][label] += 1
        entry[1][pred] += 1
    true_labels, pred_labels = [], []
    for _, (label_counts, pred_counts) in votes.items():
        true_labels.append(label_counts.index(max(label_counts)))
        pred_labels.append(pred_counts.index(max(pred_counts)))
    return true_labels, pred_labels


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
    parser.add_argument("--epochs", type=int, default=8)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--max-len", type=int, default=384)
    parser.add_argument("--lr", type=float, default=2e-5)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--export-only", action="store_true",
                        help="skip training; export the saved model and rebuild the probe")
    args = parser.parse_args()

    if args.export_only:
        samples = load_samples(args.dataset)
        probe_path = Path(args.out_dir) / "features.json"
        existing = json.loads(probe_path.read_text(encoding="utf-8")) if probe_path.exists() else {}
        temperature = existing.get("temperature", 1.0)
        export_onnx(args.out_dir, args.model_name)
        train_feature_probe(samples, args.out_dir, temperature=temperature)
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

    train_loader = make_loader(CefrDataset(train, tokenizer, args.max_len), args.batch_size, shuffle=True, device=device)
    val_loader = make_loader(CefrDataset(val, tokenizer, args.max_len), args.batch_size, shuffle=False, device=device)
    test_loader = make_loader(CefrDataset(test, tokenizer, args.max_len), args.batch_size, shuffle=False, device=device)

    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=0.01)
    steps = args.epochs * len(train_loader)
    scheduler = get_linear_schedule_with_warmup(optimizer, int(0.1 * steps), steps)

    best_f1 = 0.0
    epochs_without_gain = 0
    for epoch in range(1, args.epochs + 1):
        loss = train_epoch(model, train_loader, optimizer, scheduler, criterion, device)
        labels, preds, text_ids = evaluate(model, val_loader, device)
        chunk_f1 = f1_score(labels, preds, average="macro")
        true_t, pred_t = text_level(labels, preds, text_ids)
        text_f1 = f1_score(true_t, pred_t, average="macro")
        print(f"epoch {epoch}: loss={loss:.4f} chunk_f1={chunk_f1:.4f} text_f1={text_f1:.4f}")
        if text_f1 > best_f1:
            best_f1 = text_f1
            epochs_without_gain = 0
            model.save_pretrained(args.out_dir)
            tokenizer.save_pretrained(args.out_dir)
        else:
            epochs_without_gain += 1
            if epochs_without_gain >= 3:
                print("early stopping")
                break

    model = BertForSequenceClassification.from_pretrained(args.out_dir)
    model.to(device)
    labels, preds, text_ids = evaluate(model, test_loader, device)
    print("=== test (chunk) ===")
    print(classification_report(labels, preds, target_names=LEVELS, digits=3))
    true_t, pred_t = text_level(labels, preds, text_ids)
    print("=== test (text) ===")
    print(classification_report(true_t, pred_t, target_names=LEVELS, digits=3))
    print(f"chunk_macro_f1={f1_score(labels, preds, average='macro'):.4f} "
          f"text_macro_f1={f1_score(true_t, pred_t, average='macro'):.4f}")

    temperature = fit_temperature(model, val_loader, device)
    print(f"temperature={temperature:.2f}")

    export_onnx(args.out_dir, args.model_name)
    train_feature_probe(samples, args.out_dir, temperature=temperature)


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


def train_feature_probe(samples, out_dir, temperature=1.0):
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
        "blendAlpha": 0.85,
        "temperature": temperature,
    }
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    (out / "features.json").write_text(
        json.dumps(features_json, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("feature probe written to", out / "features.json")


def fit_temperature(model, loader, device):
    """
    Fits a softmax temperature on the validation set to calibrate confidence.
    """
    model.eval()
    all_logits, all_labels = [], []
    with torch.no_grad():
        for batch in loader:
            out = model(
                input_ids=batch["input_ids"].to(device),
                attention_mask=batch["attention_mask"].to(device),
                token_type_ids=batch["token_type_ids"].to(device),
            )
            all_logits.append(out.logits)
            all_labels.append(batch["labels"])
    logits = torch.cat(all_logits).float()
    labels = torch.cat(all_labels).to(device)

    best_t, best_nll = 1.0, float("inf")
    for candidate in [x / 10 for x in range(5, 31)]:
        scaled = torch.log_softmax(logits / candidate, dim=-1)
        nll = -scaled.gather(1, labels.unsqueeze(1)).mean().item()
        if nll < best_nll:
            best_nll, best_t = nll, candidate
    return best_t




if __name__ == "__main__":
    main()
