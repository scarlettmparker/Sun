#!/usr/bin/env python3
"""
Builds app/model/greeklex.json.gz from the GreekLex word-frequency list.
"""

import gzip
import io
import json
import sys
import zipfile
from pathlib import Path

LOWER = "GreekLex_v101/GreekLex_LowerCase.txt"


def load_frequencies(zip_path, member):
    """
    Returns a word -> per-million-frequency map from a GreekLex TSV.
    """
    frequencies = {}
    with zipfile.ZipFile(zip_path) as zf:
        with zf.open(member) as f:
            text = io.TextIOWrapper(f, encoding="utf-8")
            header = text.readline()
            columns = header.rstrip("\n").split("\t")
            assert "WordFreq" in columns, columns
            word_index = columns.index("Word")
            freq_index = columns.index("WordFreq")
            for line in text:
                parts = line.rstrip("\n").split("\t")
                if len(parts) <= freq_index:
                    continue
                freq = parts[freq_index].strip()
                if not freq:
                    continue
                try:
                    frequencies[parts[word_index]] = float(freq)
                except ValueError:
                    continue
    return frequencies


def main():
    zip_path = sys.argv[1] if len(sys.argv) > 1 else str(
        Path.home() / "Downloads" / "GreekLex_v101.zip")
    out = Path(__file__).resolve().parent.parent / "app" / "model" / "greeklex.json.gz"
    frequencies = load_frequencies(zip_path, LOWER)
    out.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(out, "wt", encoding="utf-8") as f:
        json.dump(frequencies, f, ensure_ascii=False, separators=(",", ":"))
    print(f"wrote {len(frequencies)} words to {out}")


if __name__ == "__main__":
    main()
