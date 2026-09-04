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

  const csp = `${buildCspHeader(nonce)}; ${buildTrustedTypesDirective()}`;
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
