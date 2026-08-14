#!/usr/bin/env python3
"""
Crawls paidika-paramythia.gr and extracts full story texts.

Stories are assigned a soft CEFR label of B2 or C1 (split on median length),
matching the site's roughly B2-C1 difficulty, for use as training data.
"""

import argparse
import json
import re
import sys
import time
from collections import Counter
from pathlib import Path

import requests
from bs4 import BeautifulSoup

BASE = "https://www.paidika-paramythia.gr"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
}
CATEGORIES = [
    "klasika-paramythia",
    "paradosiaka-paramythia",
    "nea-paramythia",
    "mythologia",
]


def story_links(session, category, page):
    """
    Returns the story URLs listed on a category page.
    """
    url = f"{BASE}/fairy-tale-category/{category}"
    if page:
        url += f"?page={page}"
    resp = session.get(url, headers=HEADERS, timeout=10)
    resp.raise_for_status()
    return sorted(set(re.findall(r'href="(/story/\d+/[^"]+)"', resp.text)))


def fetch_story(session, path):
    """
    Fetches a story page and extracts its title and body text.
    """
    resp = session.get(BASE + path, headers=HEADERS, timeout=10)
    resp.raise_for_status()
    soup = BeautifulSoup(resp.text, "html.parser")
    title_el = soup.select_one("h1")
    title = title_el.get_text(strip=True) if title_el else path.rsplit("/", 1)[-1]
    candidates = soup.select("div.text-content")
    body = max(candidates, key=lambda d: len(d.get_text(" ", strip=True)), default=None)
    if body is None:
        return None
    text = body.get_text("\n", strip=True)
    if len(text) < 300:
        return None
    return {"title": title, "content": text}


def main():
    """
    Crawls the site and writes the scraped stories to a JSONL file.
    """
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="data/paidika.jsonl")
    parser.add_argument("--max-stories", type=int, default=400)
    parser.add_argument("--delay", type=float, default=0.4)
    args = parser.parse_args()

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    print("starting scrape", flush=True)
    session = requests.Session()
    seen = {}
    for category in CATEGORIES:
        page = 0
        while True:
            try:
                print(f"category {category} page {page}...", flush=True)
                links = story_links(session, category, page)
            except requests.RequestException as e:
                print(f"category {category} page {page}: {e}", file=sys.stderr)
                break
            if not links:
                break
            new_ids = [int(re.search(r"/story/(\d+)/", l).group(1)) for l in links if int(re.search(r"/story/(\d+)/", l).group(1)) not in seen]
            if not new_ids:
                break
            for link in links:
                story_id = int(re.search(r"/story/(\d+)/", link).group(1))
                if story_id in seen:
                    continue
                try:
                    print(f"fetching {link}...", flush=True)
                    story = fetch_story(session, link)
                except requests.RequestException as e:
                    print(f"story {link}: {e}", file=sys.stderr)
                    continue
                if story and len(story["content"]) > 300:
                    seen[story_id] = story
                    print(f"scraped {story_id}: {story['title']} ({len(story['content'])} chars)",
                          file=sys.stderr)
                if len(seen) >= args.max_stories:
                    break
                time.sleep(args.delay)
            if len(seen) >= args.max_stories:
                break
            page += 1
        if len(seen) >= args.max_stories:
            break

    stories = list(seen.values())
    lengths = [len(s["content"]) for s in stories]
    median = sorted(lengths)[len(lengths) // 2] if lengths else 0
    for story in stories:
        story["level"] = "C1" if len(story["content"]) > median else "B2"
    with open(out, "w", encoding="utf-8") as f:
        for story in stories:
            f.write(json.dumps(story, ensure_ascii=False) + "\n")
    print(f"wrote {len(stories)} stories to {out}")
    print(Counter(s["level"] for s in stories))


if __name__ == "__main__":
    main()
