import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.features import extract_features  # noqa: E402


def test_extract_features_counts_common_stats():
    """
    Computes sentence, token, and rarity stats from a sample text.
    """
    text = "Ο γάτος κοιμάται. Ο γάτος τρέχει γρήγορα."
    frequency = {"ο": 10000, "γάτος": 500, "κοιμάται": 100, "τρέχει": 50, "γρήγορα": 20}
    features = extract_features(text, frequency)

    assert features["avgSentenceLength"] == 3.5
    assert features["typeTokenRatio"] > 0
    assert features["rareWordRatio"] == 0.0
    assert features["outOfLexiconRatio"] == 0.0
    assert features["meanLogFreq"] > 0


def test_extract_features_counts_rare_words():
    """
    Flags words missing from the frequency lexicon as out-of-lexicon.
    """
    features = extract_features("Ο γάτος ξιφουλκεί σκιάχτρο", {"ο": 10000, "γάτος": 500})
    assert features["outOfLexiconRatio"] > 0
    assert features["rareWordRatio"] > 0


def test_extract_features_empty_text():
    """
    Returns zeroed features for empty input.
    """
    features = extract_features("", set())
    assert features["avgSentenceLength"] == 0.0
