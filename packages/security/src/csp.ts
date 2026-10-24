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
  /**
   * Alias for additionalConnectSrc.
   */
  extraConnectSrc?: string | string[];
  /**
   * Alias for additionalConnectSrc.
   */
  connectSrcExtras?: string | string[];
  /**
   * Alias for additionalConnectSrc.
   */
  connectSrc?: string | string[];
};

/**
 * Collapses supported spellings of extra connect-src origins into one list.
 *
 * @param inputs - Candidate origin lists in any supported shape.
 */
function normalizeConnectSrcExtras(...inputs: unknown[]): string[] {
  const values: string[] = [];
  for (const input of inputs) {
    if (input == null) continue;
    if (Array.isArray(input)) {
      values.push(...input.map((value) => String(value)));
      continue;
    }
    if (typeof input === "string") {
      values.push(input);
      continue;
    }
    if (typeof input === "object") {
      const record = input as Record<string, unknown>;
      for (const key of [
        "additionalConnectSrc",
        "extraConnectSrc",
        "connectSrcExtras",
        "connectSrc",
      ]) {
        const value = record[key];
        if (value == null) continue;
        if (Array.isArray(value)) {
          values.push(...value.map((entry) => String(entry)));
        } else {
          values.push(String(value));
        }
      }
    }
  }
  return Array.from(
    new Set(values.map((value) => value.trim()).filter(Boolean)),
  );
}

/**
 * Builds a strict Content-Security-Policy header value using a nonce.
 *
 * @param nonce - Per-request base64 nonce to allow inline scripts.
 * @param additionalConnectSrc - Extra origins to append to connect-src.
 */
export function buildCspHeader(
  nonce: string,
  additionalConnectSrc?: string | string[],
): string;

/**
 * Builds a strict Content-Security-Policy header value using a nonce.
 *
 * @param options - Nonce plus any extra connect-src origins.
 */
export function buildCspHeader(options: CspHeaderOptions): string;

export function buildCspHeader(
  nonceOrOptions: string | CspHeaderOptions,
  additionalConnectSrc?: string | string[],
): string {
  const nonce =
    typeof nonceOrOptions === "string" ? nonceOrOptions : nonceOrOptions.nonce;
  const extras = normalizeConnectSrcExtras(
    typeof nonceOrOptions === "string" ? additionalConnectSrc : nonceOrOptions,
    typeof nonceOrOptions === "string" ? undefined : additionalConnectSrc,
  );
  const connectSrc = [
    "'self'",
    "https://*.scarlettparker.co.uk",
    ...extras,
  ].join(" ");

  const directives = [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic' https: 'unsafe-inline'`,
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
    "worker-src 'self' blob:",
  ];
  return directives.join("; ");
}

/**
 * Builds Trusted Types + require-trusted-types-for directive to append to CSP.
 */
export function buildTrustedTypesDirective(): string {
  return "require-trusted-types-for 'script'; trusted-types default dompurify";
}
