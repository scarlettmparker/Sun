/**
 * Builds a strong Strict-Transport-Security header.
 *
 * max-age 1 year + includeSubDomains + preload satisfies Lighthouse
 * "Use a strong HSTS policy" (requires >=15552000 and includeSubDomains).
 */
export function buildHstsHeader(): string {
  return "max-age=31536000; includeSubDomains; preload";
}
