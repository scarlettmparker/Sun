import { randomBytes } from "crypto";

/**
 * Generates a per-request CSP nonce (base64, 16 bytes).
 */
export function generateCspNonce(): string {
  return randomBytes(16).toString("base64");
}

/**
 * Options accepted by {@link buildCspHeader} when passed as an object.
 */
export type CspHeaderOptions = {
  /**
   * Per-request base64 nonce to allow inline scripts.
   */
  nonce: string;
  /**
   * Additional origins to append to connect-src.
   */
  additionalConnectSrc?: string | string[];
};

/**
 * Normalizes extra connect-src origins into one de-duplicated list.
 *
 * @param extras Additional origins, or a single origin.
 */
function normalizeConnectSrc(extras?: string | string[]): string[] {
  if (!extras) return [];
  return Array.from(
    new Set(
      (Array.isArray(extras) ? extras : [extras])
        .map((value) => value.trim())
        .filter(Boolean),
    ),
  );
}

/**
 * Builds a strict Content-Security-Policy header value using a nonce.
 *
 * @param nonce Per-request base64 nonce to allow inline scripts.
 * @param additionalConnectSrc Extra origins to append to connect-src.
 */
export function buildCspHeader(
  nonce: string,
  additionalConnectSrc?: string | string[],
): string;
/**
 * Builds a strict Content-Security-Policy header value using a nonce.
 *
 * @param options Nonce plus any extra connect-src origins.
 */
export function buildCspHeader(options: CspHeaderOptions): string;
export function buildCspHeader(
  nonceOrOptions: string | CspHeaderOptions,
  additionalConnectSrc?: string | string[],
): string {
  const nonce =
    typeof nonceOrOptions === "string" ? nonceOrOptions : nonceOrOptions.nonce;
  const extras = normalizeConnectSrc(
    typeof nonceOrOptions === "string"
      ? additionalConnectSrc
      : nonceOrOptions.additionalConnectSrc,
  );
  const connectSrc = [
    "'self'",
    "https://*.scarlettparker.co.uk",
    ...extras,
  ].join(" ");

  const directives = [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic' https: 'unsafe-inline' 'wasm-unsafe-eval'`,
    "style-src 'self' https: 'unsafe-inline'",
    "font-src 'self' data: https:",
    "img-src 'self' data: https: blob:",
    "media-src 'self' https: blob:",
    `connect-src ${connectSrc}`,
    "frame-src 'self' https://open.spotify.com",
    "frame-ancestors 'none'",
    "base-uri 'none'",
    "form-action 'self'",
    "object-src 'none'",
    "worker-src 'self' blob: data:",
  ];
  return directives.join("; ");
}

/**
 * Builds Trusted Types + require-trusted-types-for directive to append to CSP.
 */
export function buildTrustedTypesDirective(): string {
  return "require-trusted-types-for 'script'; trusted-types default dompurify";
}