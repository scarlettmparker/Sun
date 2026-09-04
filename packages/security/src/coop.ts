/**
 * Builds Cross-Origin-Opener-Policy header value.
 *
 * @param allowPopups - Whether to allow popups via same-origin-allow-popups.
 */
export function buildCoopHeader(allowPopups = false): string {
  return allowPopups ? "same-origin-allow-popups" : "same-origin";
}

/**
 * Builds Cross-Origin-Embedder-Policy header value.
 *
 * @param credentialless - Whether to use credentialless instead of require-corp.
 */
export function buildCoepHeader(credentialless = true): string {
  return credentialless ? "credentialless" : "require-corp";
}

/**
 * Builds Cross-Origin-Resource-Policy header value.
 *
 * @param policy - The CORP policy to apply.
 */
export function buildCorpHeader(
  policy: "same-site" | "same-origin" | "cross-origin" = "same-site",
): string {
  return policy;
}
