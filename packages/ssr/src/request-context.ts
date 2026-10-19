/**
 * @fileoverview Per-request AsyncLocalStorage context shared by the Fastify dev
 * server and the Bun.serve production server.
 */

import { AsyncLocalStorage } from "node:async_hooks";
import { globalSingleton } from "./global-state";
import {
  setRequestCacheProvider,
  setRequestCookieProvider,
  setRequestIpProvider,
} from "./page-data";
import type { CacheRecord } from "./page-data";

const requestCacheAls = globalSingleton(
  "ssr.requestCacheAls",
  () => new AsyncLocalStorage<Map<string, CacheRecord>>(),
);
setRequestCacheProvider(() => requestCacheAls.getStore() ?? null);

const requestCookieAls = globalSingleton(
  "ssr.requestCookieAls",
  () => new AsyncLocalStorage<string | undefined>(),
);
setRequestCookieProvider(() => requestCookieAls.getStore() ?? undefined);

const requestIpAls = globalSingleton(
  "ssr.requestIpAls",
  () => new AsyncLocalStorage<string | undefined>(),
);
setRequestIpProvider(() => requestIpAls.getStore() ?? undefined);

/**
 * Enters a fresh per-request context for the current execution.
 *
 * @param context Cookie header and client IP for the request.
 */
export function enterRequestContext(context: { cookie?: string; ip?: string }): void {
  requestCacheAls.enterWith(new Map());
  requestCookieAls.enterWith(context.cookie);
  requestIpAls.enterWith(context.ip);
}

/**
 * Runs a function inside a fresh per-request context.
 *
 * @param context Cookie header and client IP for the request.
 * @param fn Work to run within the request context.
 * @returns Whatever `fn` returns.
 */
export function runWithRequestContext<T>(
  context: { cookie?: string; ip?: string },
  fn: () => T,
): T {
  return requestCacheAls.run(new Map(), () =>
    requestCookieAls.run(context.cookie, () => requestIpAls.run(context.ip, fn)),
  );
}

/**
 * Seeds the current request cache with a resolved entry.
 *
 * @param key Cache key.
 * @param result Merged result object.
 */
export function seedRequestCache(key: string, result: Record<string, unknown>): void {
  requestCacheAls.getStore()?.set(key, {
    status: "resolved",
    result,
    timestamp: Date.now(),
  });
}
