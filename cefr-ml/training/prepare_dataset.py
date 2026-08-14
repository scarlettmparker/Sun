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
    parser.add_argument("--lenguia", default="data/lenguia.jsonl")
    parser.add_argument("--out", default="data/dataset.jsonl")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--max-per-level", type=int, default=300)
    parser.add_argument("--min-words", type=int, default=8)
    args = parser.parse_args()

    random.seed(args.seed)
    samples = []

    def add_source(path, source):
        for idx, line in enumerate(Path(path).read_text(encoding="utf-8").splitlines()):
            row = json.loads(line)
            level = row["level"].upper()
            if level not in LEVELS or not row["content"]:
                continue
            text_id = f"{source}:{idx}"
            for chunk in chunk_text(row["content"]):
                if len(chunk.split()) >= args.min_words:
                    samples.append({"level": level, "source": source, "text": chunk, "textId": text_id})

    add_source(args.texts, "reader")
    if Path(args.paidika).exists():
        add_source(args.paidika, "paidika")
    if Path(args.lenguia).exists():
        add_source(args.lenguia, "lenguia")

    by_level = Counter(s["level"] for s in samples)
    print("samples per level (raw):", dict(sorted(by_level.items())))

    # Cap each level, keeping trusted reader/lenguia chunks ahead of paidika chunks.
    capped = []
    for level in LEVELS:
        pool = [s for s in samples if s["level"] == level]
        trusted = [s for s in pool if s["source"] in ("reader", "lenguia")]
        paidika = [s for s in pool if s["source"] == "paidika"]
        random.shuffle(paidika)
        if len(trusted) >= args.max_per_level:
            random.shuffle(trusted)
            capped += trusted[: args.max_per_level]
        else:
            remaining = args.max_per_level - len(trusted)
            capped += trusted + paidika[:remaining]
    samples = capped
    print("samples per level (capped):", dict(sorted(Counter(s["level"] for s in samples).items())))

    # Stratified split.
    random.shuffle(samples)
    # Split whole texts, never chunks, so no text spans train and test.
    texts_by_level = {level: [] for level in LEVELS}
    for sample in samples:
        if sample["textId"] not in texts_by_level[sample["level"]]:
            texts_by_level[sample["level"]].append(sample["textId"])

    train_ids, val_ids, test_ids = [], [], []
    for level in LEVELS:
        text_ids = texts_by_level[level]
        random.shuffle(text_ids)
        n = len(text_ids)
        n_val = max(1, round(n * 0.1))
        n_test = max(1, round(n * 0.1))
        train_ids += text_ids[: n - n_val - n_test]
        val_ids += text_ids[n - n_val - n_test : n - n_test]
        test_ids += text_ids[n - n_test :]
    train_set, val_set = set(train_ids), set(val_ids)

    def split_of(sample):
        if sample["textId"] in train_set:
            return "train"
        if sample["textId"] in val_set:
            return "val"
        return "test"

    for sample in samples:
        sample["split"] = split_of(sample)

    counts = Counter(s["split"] for s in samples)
    print("chunks train/val/test:", counts["train"], counts["val"], counts["test"])
    print("texts train/val/test:", len(train_set), len(val_set), len(test_ids))

    with open(args.out, "w", encoding="utf-8") as f:
        for s in samples:
            f.write(json.dumps(s, ensure_ascii=False) + "\n")


if __name__ == "__main__":
    main()
