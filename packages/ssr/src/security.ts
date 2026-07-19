import crypto from "node:crypto";
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { CSRF_COOKIE, CSRF_FIELD, CSRF_HEADER } from "./csrf";

/**
 * Methods that cannot mutate state, so they skip both gates.
 */
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);
/**
 * Origin hostnames permitted to make state-changing requests.
 */
const DEFAULT_ALLOWED_ORIGINS = [
  "scarlettparker.co.uk",
  "localhost",
  "127.0.0.1",
];
/**
 * CSRF cookie lifetime in seconds (24h).
 */
const CSRF_MAX_AGE = 86_400;

export interface SecurityConfig {
  /**
   * HMAC key signing CSRF tokens. When unset, CSRF checks are skipped.
   */
  clientSecret?: string;
  /**
   * Hostname suffixes allowed by the origin gate.
   */
  allowedOrigins?: string[];
  /**
   * Adds the Secure cookie attribute when true.
   */
  isProduction?: boolean;
}

/**
 * Checks the request's Origin (or Referer fallback) host against the suffixes.
 *
 * @param request The incoming Fastify request.
 * @param suffixes Permitted hostname suffixes.
 * @returns True when the host matches an allowed suffix.
 */
function isOriginAllowed(
  request: FastifyRequest,
  suffixes: string[],
): boolean {
  const raw = request.headers.origin ?? request.headers.referer;
  if (!raw || typeof raw !== "string") return false;
  let hostname: string;
  try {
    hostname = new URL(raw).hostname.toLowerCase();
  } catch {
    return false;
  }
  return suffixes.some(
    (suffix) => hostname === suffix || hostname.endsWith("." + suffix),
  );
}

/**
 * Signs a nonce into its transmitted form.
 *
 * @param nonce The random token payload.
 * @param secret The HMAC key.
 * @returns The signed token `${nonce}.${mac}`.
 */
function sign(nonce: string, secret: string): string {
  const mac = crypto
    .createHmac("sha256", secret)
    .update(nonce)
    .digest("base64url");
  return `${nonce}.${mac}`;
}

/**
 * Compares two strings in constant time.
 *
 * @param a First string.
 * @param b Second string.
 * @returns False on length mismatch, otherwise the timed comparison result.
 */
function constantTimeEqual(a: string, b: string): boolean {
  const ba = Buffer.from(a);
  const bb = Buffer.from(b);
  if (ba.length !== bb.length) return false;
  return crypto.timingSafeEqual(ba, bb);
}

/**
 * Verifies a signed token by recomputing its signature.
 *
 * @param token The submitted `${nonce}.${mac}` token, or undefined.
 * @param secret The HMAC key.
 * @returns True when the signature matches.
 */
function verifyToken(token: string | undefined, secret: string): boolean {
  if (!token) return false;
  const dot = token.indexOf(".");
  if (dot <= 0 || dot === token.length - 1) return false;
  return constantTimeEqual(token, sign(token.slice(0, dot), secret));
}

/**
 * Sets a fresh signed CSRF token cookie on the reply.
 *
 * @param reply The Fastify reply to attach the cookie to.
 * @param secret The HMAC key.
 * @param isProduction Adds the Secure attribute when true.
 */
function issueCsrfCookie(
  reply: FastifyReply,
  secret: string,
  isProduction: boolean,
): void {
  const nonce = crypto.randomBytes(18).toString("base64url");
  const flags = [
    "Path=/",
    "SameSite=Lax",
    isProduction ? "Secure" : "",
    `Max-Age=${CSRF_MAX_AGE}`,
  ]
    .filter(Boolean)
    .join("; ");
  reply.header(
    "Set-Cookie",
    `${CSRF_COOKIE}=${encodeURIComponent(sign(nonce, secret))}; ${flags}`,
  );
}

/**
 * Checks whether the request already carries a CSRF cookie.
 *
 * @param request The incoming Fastify request.
 * @returns True when a CSRF cookie is present.
 */
function hasCsrfCookie(request: FastifyRequest): boolean {
  const cookie = request.headers.cookie;
  if (!cookie) return false;
  return cookie
    .split(/;\s*/)
    .some((part) => part.startsWith(CSRF_COOKIE + "="));
}

/**
 * Reads the submitted token from the header, falling back to the form field.
 *
 * @param request The incoming Fastify request.
 * @returns The token string, or undefined when neither is present.
 */
function submittedToken(request: FastifyRequest): string | undefined {
  const header = request.headers[CSRF_HEADER];
  if (typeof header === "string") return header;
  const field = (request.body as Record<string, unknown> | undefined)?.[
    CSRF_FIELD
  ];
  return typeof field === "string" ? field : undefined;
}

/**
 * Registers the origin gate and signed-token CSRF protection. The origin gate
 * always applies; CSRF issuance and verification run only when a clientSecret is
 * configured.
 *
 * @param app The Fastify instance to secure.
 * @param config Secret, origin allowlist, and environment flags.
 */
export function registerSecurity(
  app: FastifyInstance,
  config: SecurityConfig,
): void {
  const suffixes = config.allowedOrigins?.length
    ? config.allowedOrigins
    : DEFAULT_ALLOWED_ORIGINS;
  const secret = config.clientSecret;

  app.addHook("onRequest", async (request, reply) => {
    if (SAFE_METHODS.has(request.method.toUpperCase())) return;
    if (!isOriginAllowed(request, suffixes)) {
      reply.code(403).send({ error: "Origin not allowed" });
    }
  });

  if (!secret) return;

  app.addHook("preHandler", async (request, reply) => {
    if (!hasCsrfCookie(request)) {
      issueCsrfCookie(reply, secret, Boolean(config.isProduction));
    }
    if (SAFE_METHODS.has(request.method.toUpperCase())) return;
    if (!verifyToken(submittedToken(request), secret)) {
      reply.code(403).send({ error: "Invalid CSRF token" });
    }
  });
}
