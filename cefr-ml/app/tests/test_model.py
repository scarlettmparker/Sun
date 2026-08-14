import sys
from pathlib import Path

import numpy as np
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.model import CefrClassifier, LEVELS, MAX_TOKENS  # noqa: E402

LONG_TEXT = "Η ζωή στην Αθήνα είναι γεμάτη ενδιαφέροντα πράγματα. " * 60


@pytest.fixture(scope="module")
def classifier():
    """
    Loads the model once for the whole module.
    """
    return CefrClassifier()


def _count_tokens(classifier, text):
    """
    Counts real tokens, ignoring baked-in padding.
    """
    ids = classifier.tokenizer.encode(text, add_special_tokens=True).ids
    return len(ids) - classifier._trailing_pads(ids)


def test_chunks_pack_sentences(classifier):
    """
    Packs many short sentences into a few full-length chunks.
    """
    chunks = classifier._chunks(LONG_TEXT)
    assert 1 < len(chunks) < 60
    for chunk in chunks:
        assert _count_tokens(classifier, chunk) <= MAX_TOKENS


def test_chunks_preserve_content(classifier):
    """
    Keeps every word of the input across the chunks.
    """
    chunks = classifier._chunks(LONG_TEXT)
    joined = " ".join(chunks)
    assert len(joined.split()) == len(LONG_TEXT.split())


def test_batched_matches_single(classifier):
    """
    Predicts the same probabilities batched as one pass or chunk by chunk.
    """
    chunks = classifier._chunks(LONG_TEXT)
    batched = classifier._run_batch(classifier._encode(chunks))
    singles = np.vstack([
        classifier._run_batch(classifier._encode([chunk])) for chunk in chunks
    ])
    exp = lambda logits: np.exp(logits - logits.max(axis=-1, keepdims=True))
    batched = exp(batched) / exp(batched).sum(axis=-1, keepdims=True)
    singles = exp(singles) / exp(singles).sum(axis=-1, keepdims=True)
    assert batched.shape == singles.shape
    np.testing.assert_allclose(batched, singles, atol=1e-3)


def test_classify_returns_assessment(classifier):
    """
    Returns a level, confidence, and factor breakdown.
    """
    result = classifier.classify("Η γάτα κοιμάται.")
    assert result["level"] in LEVELS
    assert 0.0 <= result["confidence"] <= 1.0
    assert len(result["probabilities"]) == 6
    assert len(result["factors"]) > 0
