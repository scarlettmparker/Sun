import { AsyncLocalStorage } from "node:async_hooks";
import { generateCspNonce } from "@sun/security";

const cspNonceAls = new AsyncLocalStorage<string>();

/**
 * Gets the per-request CSP nonce, if available.
 */
export function getCspNonce(): string | undefined {
  return cspNonceAls.getStore();
}

/**
 * Runs a function with a fresh CSP nonce for the current request.
 *
 * @param fn - Function to run with the nonce context.
 */
export function withCspNonce<T>(fn: () => T): T {
  return cspNonceAls.run(generateCspNonce(), fn);
}

/**
 * Enters a new CSP nonce context for the current request.
 * Used in Fastify onRequest hook where run() is not practical.
 */
export function enterCspNonce(): void {
  cspNonceAls.enterWith(generateCspNonce());
}
