#!/usr/bin/env python3
"""
Filters paidika stories whose vocabulary frequency contradicts their label.
"""

import gzip
import json
import sys
from collections import defaultdict
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "app"))
from features import extract_features  # noqa: E402

LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]
LEVEL_INDEX = {level: i for i, level in enumerate(LEVELS)}


def load_texts(path):
    texts = []
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        texts.append(json.loads(line))
    return texts


def vocab_scores(texts, frequency):
    scores = {}
    for text in texts:
        features = extract_features(text["content"], frequency)
        scores[text.get("title", "")] = {
            "rareWordRatio": features["rareWordRatio"],
            "level": text["level"].upper(),
        }
    return scores


def main():
    frequency = json.loads(
        gzip.open(ROOT / "app" / "model" / "greeklex.json.gz", "rt", encoding="utf-8").read()
    )
    trusted = load_texts(ROOT / "data" / "reader_texts.jsonl")
    paidika = load_texts(ROOT / "data" / "paidika.jsonl")

    scores = vocab_scores(trusted, frequency)
    by_level = defaultdict(list)
    for entry in scores.values():
        by_level[entry["level"]].append(entry["rareWordRatio"])
    stats = {level: (np.mean(v), np.std(v) or 1e-9) for level, v in by_level.items()}
    print("trusted rareWordRatio per level:")
    for level in LEVELS:
        mean, std = stats.get(level, (0, 1))
        print(f"  {level}: mean={mean:.3f} std={std:.3f} n={len(by_level.get(level, []))}")

    def predicted_level(score):
        return min(LEVELS, key=lambda lvl: abs(score - stats[lvl][0]))

    kept, dropped = [], 0
    for story in paidika:
        features = extract_features(story["content"], frequency)
        score = features["rareWordRatio"]
        label = story["level"].upper()
        predicted = predicted_level(score)
        label_idx = LEVEL_INDEX[label]
        pred_idx = LEVEL_INDEX[predicted]
        if label in ("B2", "C1") and pred_idx < label_idx - 1:
            dropped += 1
            continue
        kept.append(story)

    out = ROOT / "data" / "paidika_valid.jsonl"
    with open(out, "w", encoding="utf-8") as f:
        for story in kept:
            f.write(json.dumps(story, ensure_ascii=False) + "\n")
    print(f"paidika: {len(paidika)} -> kept {len(kept)}, dropped {dropped} as label noise -> {out}")


if __name__ == "__main__":
    main()
