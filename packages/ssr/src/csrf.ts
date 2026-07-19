/**
 * Readable cookie holding the signed CSRF token.
 */
export const CSRF_COOKIE = "csrf_token";
/**
 * Header the browser echoes the token back on for fetch-based POSTs.
 */
export const CSRF_HEADER = "x-csrf-token";
/**
 * Form field carrying the token on native (PRG) form posts.
 */
export const CSRF_FIELD = "_csrf";

/**
 * Reads the CSRF token from the browser cookie.
 *
 * @returns The token, or undefined outside a browser or before issue.
 */
export function getCsrfToken(): string | undefined {
  if (typeof document === "undefined" || !document.cookie) return undefined;
  for (const part of document.cookie.split(/;\s*/)) {
    const index = part.indexOf("=");
    if (index < 0) continue;
    if (part.slice(0, index).trim() === CSRF_COOKIE) {
      return decodeURIComponent(part.slice(index + 1));
    }
  }
  return undefined;
}
