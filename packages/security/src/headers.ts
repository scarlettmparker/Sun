import { buildCspHeader, buildTrustedTypesDirective } from "./csp";
import { buildHstsHeader } from "./hsts";
import { buildCoopHeader, buildCoepHeader, buildCorpHeader } from "./coop";

export type SecurityHeadersOptions = {
  /**
   * Whether the app is running in production.
   */
  isProduction: boolean;
  /**
   * Per-request CSP nonce.
   */
  nonce: string;
  /**
   * Whether to allow COOP popups for Spotify embeds.
   */
  allowCoopPopups?: boolean;
  /**
   * Whether to use credentialless COEP.
   */
  useCredentiallessCoep?: boolean;
};

/**
 * Builds all security headers for an HTML or API response.
 *
 * @param options - Production flag, nonce, and COOP/COEP toggles.
 */
export function buildSecurityHeaders(
  options: SecurityHeadersOptions,
): Record<string, string> {
  const { isProduction, nonce, allowCoopPopups, useCredentiallessCoep } =
    options;

  // In dev, allow Vite HMR (unsafe-inline/eval, ws) - strict CSP would break it.
  const csp = isProduction
    ? `${buildCspHeader(nonce)}; ${buildTrustedTypesDirective()}`
    : [
        "default-src 'self'",
        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https: http: ws: wss:",
        "style-src 'self' 'unsafe-inline' https:",
        "font-src 'self' data: https:",
        "img-src 'self' data: https: blob:",
        "media-src 'self' https: blob:",
        "connect-src 'self' https: ws: wss: http:",
        "frame-src 'self' https://open.spotify.com",
        "frame-ancestors 'none'",
        "base-uri 'none'",
        "form-action 'self'",
        "object-src 'none'",
      ].join("; ");

  const headers: Record<string, string> = {
    "Content-Security-Policy": csp,
    "Cross-Origin-Opener-Policy": buildCoopHeader(allowCoopPopups),
    "Cross-Origin-Embedder-Policy": buildCoepHeader(useCredentiallessCoep),
    "Cross-Origin-Resource-Policy": buildCorpHeader("same-site"),
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    "Referrer-Policy": "strict-origin-when-cross-origin",
    "Permissions-Policy":
      "geolocation=(), camera=(), microphone=(), payment=()",
  };

  if (isProduction) {
    headers["Strict-Transport-Security"] = buildHstsHeader();
  }

  return headers;
}

/**
 * Builds security headers for static assets (fonts, hashed JS/CSS).
 *
 * @param isProduction - Whether the app is running in production.
 */
export function buildStaticAssetHeaders(
  isProduction: boolean,
): Record<string, string> {
  const headers: Record<string, string> = {
    "X-Content-Type-Options": "nosniff",
    "Cross-Origin-Resource-Policy": "same-site",
  };

  if (isProduction) {
    headers["Strict-Transport-Security"] = buildHstsHeader();
  }

  return headers;
}
