#!/usr/bin/env python3
"""
Scrapes lenguia.com's Greek reading test for CEFR-labelled passages.
"""

import html
import json
import re
from pathlib import Path

import requests

URL = "https://www.lenguia.com/tools/reading-test/greek"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36"
}
LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"}


def main():
    resp = requests.get(URL, headers=HEADERS, timeout=20)
    resp.raise_for_status()
    raw = resp.text

    levels = [(m.start(), m.group(1)) for m in re.finditer(r">([ABC][12])</span>", raw)]
    passages = [
        (m.start(), m.group(1)) for m in re.finditer(r'<p dir="auto"[^>]*>(.*?)</p>', raw, re.S)
    ]

    seen, stories = set(), []
    for start, passage in passages:
        text = html.unescape(re.sub(r"<[^>]+>", "", passage)).strip().strip("“”\" ")
        if not re.search(r"[\u03b1-\u03c9]{3}", text) or len(text) < 100:
            continue
        level = next((lv for ls, lv in reversed(levels) if ls < start), None)
        if level not in LEVELS or text in seen:
            continue
        seen.add(text)
        stories.append({"level": level, "content": text, "source": "lenguia"})

    out = Path(__file__).resolve().parent.parent / "data" / "lenguia.jsonl"
    with open(out, "w", encoding="utf-8") as f:
        for story in stories:
            f.write(json.dumps(story, ensure_ascii=False) + "\n")
    print(f"wrote {len(stories)} passages to {out}")


if __name__ == "__main__":
    main()
