"""
Interpretable linguistic features used for the CEFR factor breakdown.
"""

import math
import re

PUNCTUATION = re.compile(r"[.,;:!?—–…\"'()\[\]{}<>«»]")
WORD = re.compile(r"[^\W\d_]+")

# Words with a per-million frequency below this count rare vocabulary.
RARE_FREQ_THRESHOLD = 10.0


def split_sentences(text: str):
    """
    Splits text into sentences on terminal punctuation.
    """
    parts = re.split(r"(?<=[.!?…])\s+", text.strip())
    return [p for p in parts if p.strip()]


def extract_features(text: str, frequency):
    """
    Returns a dict of named linguistic features for a text.

    frequency maps a lowercase word to its per-million corpus frequency;
    words outside it are treated as out-of-lexicon.
    """
    sentences = split_sentences(text)
    words = [w for w in WORD.findall(text.lower()) if len(w) > 1]
    total_words = len(words)
    n_sentences = len(sentences)

    if total_words == 0:
        return {
            "textLength": float(len(text)),
            "avgSentenceLength": 0.0,
            "longSentenceRatio": 0.0,
            "avgWordLength": 0.0,
            "longWordRatio": 0.0,
            "typeTokenRatio": 0.0,
            "rareWordRatio": 0.0,
            "outOfLexiconRatio": 0.0,
            "meanLogFreq": 0.0,
            "punctuationDensity": 0.0,
        }

    sentence_lengths = [len(WORD.findall(s.lower())) for s in sentences]
    long_sentences = sum(1 for n in sentence_lengths if n > 20)
    freq_values = [frequency[w] for w in words if w in frequency]
    rare = sum(1 for w in words if frequency.get(w, 0) < RARE_FREQ_THRESHOLD)
    out_of_lexicon = sum(1 for w in words if w not in frequency)

    return {
        "textLength": float(len(text)),
        "avgSentenceLength": (sum(sentence_lengths) / n_sentences) if n_sentences else 0.0,
        "longSentenceRatio": (long_sentences / n_sentences) if n_sentences else 0.0,
        "avgWordLength": sum(len(w) for w in words) / total_words,
        "longWordRatio": sum(1 for w in words if len(w) > 8) / total_words,
        "typeTokenRatio": len(set(words)) / total_words,
        "rareWordRatio": rare / total_words,
        "outOfLexiconRatio": out_of_lexicon / total_words,
        "meanLogFreq": (sum(math.log10(v + 1) for v in freq_values) / len(freq_values))
        if freq_values
        else 0.0,
        "punctuationDensity": len(PUNCTUATION.findall(text)) * 100.0 / total_words,
    }
