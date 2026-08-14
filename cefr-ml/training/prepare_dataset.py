#!/usr/bin/env python3
"""
Builds the chunked CEFR training dataset from the reader texts + paidika scrape.
"""

import argparse
import json
import re
import random
from collections import Counter
from pathlib import Path

LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]

SENTENCE_SPLIT = re.compile(r"(?<=[.!?…])\s+")


def sentences(text: str):
    """
    Splits text into non-empty sentences.
    """
    return [s.strip() for s in SENTENCE_SPLIT.split(text) if s.strip()]


def chunk_text(text: str, target: int = 1200, max_chunks: int = 6):
    """
    Splits text into sentence-aligned chunks of ~target chars.
    """
    chunks, current = [], ""
    for sentence in sentences(text):
        if len(current) + len(sentence) + 1 > target and current:
            chunks.append(current)
            current = sentence
        else:
            current = f"{current} {sentence}".strip()
    if current:
        chunks.append(current)
    return chunks[:max_chunks]


def main():
    """
    Builds the dataset and writes it with a stratified train/val/test split.
    """
    parser = argparse.ArgumentParser()
    parser.add_argument("--texts", default="data/reader_texts.jsonl")
    parser.add_argument("--paidika", default="data/paidika.jsonl")
    parser.add_argument("--out", default="data/dataset.jsonl")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--max-per-level", type=int, default=300)
    args = parser.parse_args()

    random.seed(args.seed)
    samples = []

    for line in Path(args.texts).read_text(encoding="utf-8").splitlines():
        row = json.loads(line)
        level = row["level"].upper()
        if level not in LEVELS or not row["content"]:
            continue
        for chunk in chunk_text(row["content"]):
            samples.append({"level": level, "source": "reader", "text": chunk})

    paidika = Path(args.paidika)
    if paidika.exists():
        for line in paidika.read_text(encoding="utf-8").splitlines():
            row = json.loads(line)
            level = row["level"].upper()
            if level not in LEVELS or not row["content"]:
                continue
            for chunk in chunk_text(row["content"]):
                samples.append({"level": level, "source": "paidika", "text": chunk})

    by_level = Counter(s["level"] for s in samples)
    print("samples per level (raw):", dict(sorted(by_level.items())))

    # Cap each level, keeping trusted reader chunks ahead of paidika chunks.
    capped = []
    for level in LEVELS:
        pool = [s for s in samples if s["level"] == level]
        reader = [s for s in pool if s["source"] == "reader"]
        paidika = [s for s in pool if s["source"] == "paidika"]
        random.shuffle(paidika)
        if len(reader) >= args.max_per_level:
            random.shuffle(reader)
            capped += reader[: args.max_per_level]
        else:
            remaining = args.max_per_level - len(reader)
            capped += reader + paidika[:remaining]
    samples = capped
    print("samples per level (capped):", dict(sorted(Counter(s["level"] for s in samples).items())))

    # Stratified split.
    random.shuffle(samples)
    split_map = {}
    for sample in samples:
        split_map.setdefault(sample["level"], []).append(sample)
    train, val, test = [], [], []
    for level in LEVELS:
        pool = split_map.get(level, [])
        n = len(pool)
        n_val = max(1, round(n * 0.1))
        n_test = max(1, round(n * 0.1))
        train += pool[: n - n_val - n_test]
        val += pool[n - n_val - n_test : n - n_test]
        test += pool[n - n_test :]

    def tag(samples_, split):
        for s in samples_:
            s["split"] = split
        return samples_

    all_samples = tag(train, "train") + tag(val, "val") + tag(test, "test")
    with open(args.out, "w", encoding="utf-8") as f:
        for s in all_samples:
            f.write(json.dumps(s, ensure_ascii=False) + "\n")

    print("train/val/test:", len(train), len(val), len(test))


if __name__ == "__main__":
    main()
