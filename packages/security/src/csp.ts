import { randomBytes } from "crypto";

/**
 * Generates a per-request CSP nonce (base64, 16 bytes).
 */
export function generateCspNonce(): string {
  return randomBytes(16).toString("base64");
}

/**
 * Builds a strict Content-Security-Policy header value using a nonce.
 *
 * Allows self, nonce for inline scripts/styles, strict-dynamic for modern
 * browsers, and explicit allowances for PostHog and Spotify embeds.
 *
 * @param nonce - Per-request base64 nonce to allow inline scripts and styles.
 */
export function buildCspHeader(nonce: string): string {
  const directives = [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic' https: 'unsafe-inline'`,
    `style-src 'self' 'nonce-${nonce}' https: 'unsafe-inline' 'unsafe-hashes'`,
    "font-src 'self' data: https:",
    "img-src 'self' data: https: blob:",
    "media-src 'self' https: blob:",
    "connect-src 'self' https://*.posthog.com https://eu.i.posthog.com https://app.posthog.com https://*.scarlettparker.co.uk https:",
    "frame-src 'self' https://open.spotify.com",
    "frame-ancestors 'none'",
    "base-uri 'none'",
    "form-action 'self'",
    "object-src 'none'",
    "worker-src 'self' blob:",
  ];
  return directives.join("; ");
}

/**
 * Builds Trusted Types + require-trusted-types-for directive to append to CSP.
 * Only append when the app uses dangerouslySetInnerHTML (MarkdownViewer).
 */
export function buildTrustedTypesDirective(): string {
  return "require-trusted-types-for 'script'; trusted-types default dompurify";
}
