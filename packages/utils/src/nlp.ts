export const STOP_WORDS = new Set([
  "a",
  "an",
  "the",
  "to",
  "for",
  "of",
  "and",
  "or",
  "in",
  "on",
  "at",
  "by",
  "give",
  "me",
  "my",
  "your",
  "please",
  "what",
  "whats",
  "is",
  "are",
  "was",
  "how",
  "do",
  "does",
  "did",
  "i",
  "you",
  "we",
  "they",
  "can",
  "could",
  "would",
  "show",
  "list",
  "get",
  "want",
  "need",
  "some",
  "like",
  "just",
  "with",
]);

export type WordsMatchOptions = {
  /**
   * Max edit distance.
   */
  maxDistance?: number;
  /**
   * Min Dice bigram similarity.
   */
  minBigram?: number;
};

export type FillCorrectOptions = WordsMatchOptions & {
  /**
   * Whether to strip diacritics.
   */
  stripDiacritics?: boolean;
};

export type OverlapOptions = {
  /**
   * Token Jaccard threshold.
   */
  jaccardThreshold?: number;
  /**
   * Min consecutive token overlap.
   */
  consecutiveThreshold?: number;
};

/**
 * Tokenizes text into lowercase words, dropping punctuation and stop words.
 *
 * @param text - utterance to tokenize
 * @returns filtered tokens
 */
export function tokenize(text: string): string[] {
  return text
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .split(/\s+/)
    .filter((word) => word.length > 0 && !STOP_WORDS.has(word));
}

/**
 * Normalizes text for fill comparison.
 *
 * @param text - raw answer text
 * @param stripDiacritics - whether to strip NFD diacritics
 * @returns normalized tokens
 */
export function normalizeForFill(
  text: string,
  stripDiacritics = true,
): string[] {
  let s = text.toLowerCase();
  if (stripDiacritics) s = s.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
  return s
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .split(/\s+/)
    .filter(Boolean);
}

/**
 * Computes the Levenshtein edit distance between two strings.
 *
 * @param a - first string
 * @param b - second string
 * @returns minimum single-character edits
 */
export function levenshtein(a: string, b: string): number {
  const prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    let diag = prev[0];
    prev[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const above = prev[j];
      prev[j] =
        a[i - 1] === b[j - 1] ? diag : Math.min(diag, prev[j - 1], prev[j]) + 1;
      diag = above;
    }
  }
  return prev[b.length];
}

/**
 * Computes the Dice coefficient over the character bigrams of two strings.
 *
 * @param a - first string
 * @param b - second string
 * @returns similarity in [0, 1]
 */
export function bigramSimilarity(a: string, b: string): number {
  if (a.length < 2 || b.length < 2) return 0;
  const bigrams = (value: string): Set<string> => {
    const set = new Set<string>();
    for (let i = 0; i < value.length - 1; i++) set.add(value.slice(i, i + 2));
    return set;
  };
  const aSet = bigrams(a);
  const bSet = bigrams(b);
  let overlap = 0;
  for (const bigram of aSet) if (bSet.has(bigram)) overlap++;
  return (2 * overlap) / (aSet.size + bSet.size);
}

/**
 * Whether a token plausibly denotes a trigger word, allowing small typos.
 *
 * @param token - utterance token
 * @param word - trigger word
 * @param opts - distance and bigram thresholds
 * @returns true when within thresholds
 */
export function wordsMatch(
  token: string,
  word: string,
  opts?: WordsMatchOptions,
): boolean {
  if (token === word) return true;
  // Pure numeric tokens (years, versions) must match exactly
  if (/^\d+$/.test(token) && /^\d+$/.test(word)) return false;
  if (/^\d+$/.test(word) || /^\d+$/.test(token)) {
    return false;
  }
  const maxDistance = opts?.maxDistance ?? (word.length <= 4 ? 1 : 2);
  if (levenshtein(token, word) <= maxDistance) return true;
  const minBigram = opts?.minBigram ?? 0.65;
  return bigramSimilarity(token, word) >= minBigram;
}

/**
 * Whether a fill answer matches expected, with configurable leniency.
 *
 * @param input - learner answer
 * @param expected - correct answer
 * @param opts - thresholds and diacritic flag
 * @returns true when tokens match within thresholds
 */
export function isFillCorrect(
  input: string,
  expected: string,
  opts?: FillCorrectOptions,
): boolean {
  const strip = opts?.stripDiacritics ?? true;
  const aTokens = normalizeForFill(input, strip);
  const bTokens = normalizeForFill(expected, strip);
  if (aTokens.length === 0 || bTokens.length === 0) return false;
  if (aTokens.length !== bTokens.length) {
    const aJoined = aTokens.join(" ");
    const bJoined = bTokens.join(" ");
    if (aJoined === bJoined) return true;
    const maxDistance = opts?.maxDistance ?? 1;
    const minBigram = opts?.minBigram ?? 0.72;
    if (levenshtein(aJoined, bJoined) <= maxDistance) return true;
    return bigramSimilarity(aJoined, bJoined) >= minBigram;
  }
  const maxDistance = opts?.maxDistance;
  const minBigram = opts?.minBigram;
  for (let i = 0; i < aTokens.length; i++) {
    if (!wordsMatch(aTokens[i], bTokens[i], { maxDistance, minBigram }))
      return false;
  }
  return true;
}

/**
 * Whether two explanations overlap enough to be considered the same source span.
 *
 * @param a - first explanation
 * @param b - second explanation
 * @param opts - jaccard and consecutive thresholds
 * @returns true when overlap exceeds thresholds
 */
export function areExplanationsOverlapping(
  a: string,
  b: string,
  opts?: OverlapOptions,
): boolean {
  const jaccardThreshold = opts?.jaccardThreshold ?? 0.6;
  const consecutiveThreshold = opts?.consecutiveThreshold ?? 6;
  const aTokens = normalizeForFill(a, true);
  const bTokens = normalizeForFill(b, true);
  if (aTokens.length === 0 || bTokens.length === 0) return false;
  const aSet = new Set(aTokens);
  const bSet = new Set(bTokens);
  let inter = 0;
  for (const t of aSet) if (bSet.has(t)) inter++;
  const jaccard = inter / (aSet.size + bSet.size - inter);
  if (jaccard >= jaccardThreshold) return true;
  const bJoined = ` ${bTokens.join(" ")} `;
  let maxRun = 0;
  for (let i = 0; i < aTokens.length; i++) {
    let run = 0;
    for (let j = i; j < aTokens.length; j++) {
      const slice = ` ${aTokens.slice(i, j + 1).join(" ")} `;
      if (bJoined.includes(slice)) run = j - i + 1;
      else break;
    }
    if (run > maxRun) maxRun = run;
  }
  return maxRun >= consecutiveThreshold;
}
