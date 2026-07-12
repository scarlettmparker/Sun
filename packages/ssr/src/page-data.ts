/**
 * Generic page-data fetching utilities for SSR.
 * Provides a registry for page-data loaders and a suspense cache for data loading.
 */

/**
 * Per-request context passed to page-data loaders, so they can forward auth
 * (e.g. resolve the current user's data) to authenticated backend calls.
 */
export type PageDataContext = {
  /**
   * Raw Cookie header from the request.
   */
  cookie?: string;
};

type PageDataLoader = (
  params?: Record<string, unknown>,
  context?: PageDataContext,
) => Promise<Record<string, unknown> | null>;

// Event emitter for cache hydration
type CacheHydratedListener = () => void;
const cacheHydratedListeners: Set<CacheHydratedListener> = new Set();

export function onCacheHydrated(listener: CacheHydratedListener): () => void {
  cacheHydratedListeners.add(listener);
  return () => cacheHydratedListeners.delete(listener);
}

function notifyCacheHydrated() {
  cacheHydratedListeners.forEach((listener) => listener());
}

/**
 * Cache TTL configuration. Apps override via configurePageData() at boot.
 */
const cacheTtlConfig: {
  defaultMs: number;
  perPattern: Record<string, number>;
} = {
  defaultMs: 300000, // 5 minutes default
  perPattern: {},
};

/**
 * Configures page-data cache TTL. Call once at app boot (e.g. from the app's
 * register-loaders module) before any loader runs.
 *
 * @param opts.defaultTtlMs   Default TTL for patterns without an explicit override.
 * @param opts.perPatternTtl  Map of normalized route pattern (e.g. "/bucket/:alias") to TTL in ms.
 */
export function configurePageData(opts: {
  defaultTtlMs?: number;
  perPatternTtl?: Record<string, number>;
}): void {
  if (opts.defaultTtlMs !== undefined) {
    cacheTtlConfig.defaultMs = opts.defaultTtlMs;
  }
  if (opts.perPatternTtl) {
    Object.assign(cacheTtlConfig.perPattern, opts.perPatternTtl);
  }
}

/**
 * Cache record for React Suspense-based data loading.
 * Status of data fetches: 'pending', 'resolved', or 'rejected'.
 */
export type CacheRecord = {
  status: "pending" | "resolved" | "rejected";
  result?: Record<string, unknown>;
  promise?: Promise<unknown>;
  error?: unknown;
  timestamp?: number;
  errorAt?: number;
};

/**
 * On the server, page data is cached **per request** (via AsyncLocalStorage,
 * wired up in server.ts) so concurrent SSR renders never share/leak state and
 * each postlude contains only the keys the current render resolved. On the
 * client there is one session-level map (hydrated from the SSR postlude).
 */
const clientCache = new Map<string, CacheRecord>();
type RequestCacheProvider = () => Map<string, CacheRecord> | null;
let requestCacheProvider: RequestCacheProvider | null = null;
type RequestCookieProvider = () => string | undefined;
let requestCookieProvider: RequestCookieProvider | null = null;

/** server.ts calls this at boot to plug in the AsyncLocalStorage-backed store. */
export function setRequestCacheProvider(provider: RequestCacheProvider): void {
  requestCacheProvider = provider;
}

/** server.ts calls this at boot to plug in the request's Cookie header. */
export function setRequestCookieProvider(
  provider: RequestCookieProvider,
): void {
  requestCookieProvider = provider;
}

function activeCache(): Map<string, CacheRecord> {
  // Server inside a request → that request's map; otherwise the client/session map.
  if (typeof window === "undefined" && requestCacheProvider) {
    const store = requestCacheProvider();
    if (store) return store;
  }
  return clientCache;
}

/**
 * Capture the current request's cache reference. Call at SSR render() entry
 * (while the request context is live); the returned reference stays valid for
 * the lifetime of the render, even inside React stream callbacks.
 */
export function getRequestCache(): Map<string, CacheRecord> {
  return activeCache();
}

/** Snapshot of resolved cache entries for the SSR postlude. */
export function snapshotResolvedPageData(): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const [key, record] of activeCache().entries()) {
    if (record.status === "resolved") out[key] = record.result;
  }
  return out;
}

/**
 * Proxy that forwards every access to the active cache.
 */
export const suspenseCache = new Proxy({} as Map<string, CacheRecord>, {
  get(_target, prop) {
    const cache = activeCache();
    const value = cache[prop as keyof Map<string, CacheRecord>];
    return typeof value === "function"
      ? (value as (...args: unknown[]) => unknown).bind(cache)
      : value;
  },
});

/**
 * How long a rejected suspense-cache entry is allowed to suppress a retry.
 */
const REJECTED_RETRY_MS = 5000;

/**
 * Gets the TTL for a given cache key pattern.
 * @param pattern The route pattern
 * @returns TTL in milliseconds
 */
function getCacheTTL(pattern: string): number {
  const normalized = pattern.startsWith("/") ? pattern : "/" + pattern;
  return cacheTtlConfig.perPattern[normalized] ?? cacheTtlConfig.defaultMs;
}

/**
 * Checks if a cache entry has expired.
 */
function isCacheExpired(
  record: { timestamp?: number },
  pattern: string,
): boolean {
  if (!record.timestamp) return false;
  const ttl = getCacheTTL(pattern);
  const age = Date.now() - record.timestamp;
  return age > ttl;
}

/**
 * Hydrates the page data cache with initial data from the server.
 */
export function hydratePageData(
  initialData: Record<string, Record<string, unknown>>,
) {
  if (!initialData) {
    return;
  }

  Object.keys(initialData).forEach((key) => {
    activeCache().set(key, {
      status: "resolved",
      result: initialData[key],
      timestamp: Date.now(),
    });

    // Handle normalized keys: if the key contains a colon, normalize the pattern part
    const colonIndex = key.indexOf(":");
    if (colonIndex !== -1) {
      const patternPart = key.slice(0, colonIndex);
      const rest = key.slice(colonIndex);
      const normalizedPattern = patternPart.startsWith("/")
        ? patternPart
        : "/" + patternPart;
      const normalizedKey = `${normalizedPattern}${rest}`;
      if (!activeCache().has(normalizedKey)) {
        activeCache().set(normalizedKey, {
          status: "resolved",
          result: initialData[key],
          timestamp: Date.now(),
        });
      }
    }
  });

  notifyCacheHydrated();
}

/**
 * Registry of page data loaders.
 * Maps route patterns to their data-loading functions (multiple allowed).
 */
export const pageDataLoaders: Record<string, PageDataLoader[]> = {};

type PageDataCache = Record<
  string,
  { data: Record<string, unknown> | null; timestamp: number }
>;

/**
 * Cache for page data to avoid re-fetching on every request (production only).
 */
const pageDataCache: PageDataCache = {};

function registerPageDataLoader(pattern: string, loader: PageDataLoader): void {
  const list = pageDataLoaders[pattern] || [];
  if (!list.includes(loader)) {
    list.push(loader);
    pageDataLoaders[pattern] = list;
    Object.keys(pageDataCache).forEach((key) => {
      if (key.includes(pattern)) {
        delete pageDataCache[key];
      }
    });
  }
}

function unregisterPageDataLoader(pattern: string): void {
  delete pageDataLoaders[pattern];
  Object.keys(pageDataCache).forEach((key) => {
    if (key.includes(pattern)) {
      delete pageDataCache[key];
    }
  });
}

function hasPageDataLoader(pattern: string): boolean {
  return !!pageDataLoaders[pattern]?.length;
}

function getRegisteredPageNames(): string[] {
  return Object.keys(pageDataLoaders);
}

/**
 * Creates a normalized cache key for the given pattern and params.
 */
export function makeCacheKey(
  pattern: string,
  params?: Record<string, unknown>,
) {
  const normalized = pattern.startsWith("/") ? pattern : "/" + pattern;
  return `${normalized}:${JSON.stringify(params || {})}`;
}

/**
 * Client-side RPC, asks the server to run the registered loaders for a pattern
 * and return the merged data. Keeps all backend fetching server-side.
 */
export async function fetchPageDataRpc(
  pattern: string,
  params?: Record<string, unknown>,
): Promise<Record<string, unknown> | null> {
  try {
    const res = await fetch("/__page-data", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pattern, params }),
    });
    if (!res.ok) return null;
    const json = (await res.json()) as { data?: Record<string, unknown> };
    return (json && json.data) || null;
  } catch {
    return null;
  }
}

/**
 * Reads page data from the suspense cache, initiating a fetch if not cached.
 * Used internally for suspense-based data loading.
 */
function readPageData<T>(
  key: string,
  pattern: string,
  params?: Record<string, unknown>,
): { data: T } {
  const cacheKey = makeCacheKey(`${pattern}:${key}`, params);
  let record = activeCache().get(cacheKey);

  if (
    record &&
    record.status === "resolved" &&
    isCacheExpired(record, pattern)
  ) {
    activeCache().delete(cacheKey);
    record = undefined;
  }

  if (
    record &&
    record.status === "rejected" &&
    record.errorAt &&
    Date.now() - record.errorAt > REJECTED_RETRY_MS
  ) {
    activeCache().delete(cacheKey);
    record = undefined;
  }

  if (!record) {
    const loaders = pageDataLoaders[pattern];
    const relevantLoaders = loaders?.filter(() => true) || [];

    // On the server, no registered loaders means no data. The client never runs
    // loaders - it fetches via the /__page-data RPC instead.
    if (typeof window === "undefined" && !relevantLoaders.length) {
      return { data: null as T };
    }

    record = { status: "pending" };
    activeCache().set(cacheKey, record);

    const context: PageDataContext = {
      cookie: requestCookieProvider?.(),
    };

    const loadPromise: Promise<Record<string, unknown> | null> =
      typeof window === "undefined"
        ? Promise.all(relevantLoaders.map((l) => l(params, context))).then((results) => {
            const merged: Record<string, unknown> = {};
            for (const r of results) {
              if (r && typeof r === "object") {
                Object.assign(merged, r);
              }
            }
            return merged;
          })
        : fetchPageDataRpc(pattern, params);

    const promise = loadPromise
      .then((merged) => {
        if (!merged || merged[key] == null) {
          record!.status = "rejected";
          record!.error = new Error(`No data returned for key: ${key}`);
          record!.errorAt = Date.now();
          return null;
        }

        record!.status = "resolved";
        record!.result = merged;
        record!.timestamp = Date.now();
        return merged[key];
      })
      .catch((err) => {
        console.error(`Error fetching page data for ${key} (${pattern}):`, err);
        record!.status = "rejected";
        record!.error = err;
        record!.errorAt = Date.now();
        throw err;
      });

    record.promise = promise;
  }

  if (record!.status === "pending") {
    throw record!.promise;
  }
  if (record!.status === "rejected") {
    throw record!.error;
  }

  return { data: record!.result![key] as T };
}

/**
 * Hook to read page data.
 *
 * @param key Data key to read (e.g., 'keys')
 * @param pattern The route pattern (e.g., 'bucket/:alias/*')
 * @param params API parameters
 */
export function getPageData<T>(
  key: string,
  pattern: string,
  params?: Record<string, unknown>,
): { data: T } {
  const cacheKey = makeCacheKey(`${pattern}:${key}`, params);
  let record = activeCache().get(cacheKey);

  if (
    record &&
    record.status === "resolved" &&
    isCacheExpired(record, pattern)
  ) {
    activeCache().delete(cacheKey);
    record = undefined;
  }

  if (
    record &&
    record.status === "rejected" &&
    record.errorAt &&
    Date.now() - record.errorAt > REJECTED_RETRY_MS
  ) {
    activeCache().delete(cacheKey);
    record = undefined;
  }

  // Fallback check to support key structures populated via hydratePageData
  if (!record) {
    const legacyHydrationKey = makeCacheKey(pattern, params);
    record = activeCache().get(legacyHydrationKey);
    if (
      record &&
      record.status === "resolved" &&
      isCacheExpired(record, pattern)
    ) {
      activeCache().delete(legacyHydrationKey);
      record = undefined;
    }
  }

  if (typeof window === "undefined" || !record) {
    return readPageData(key, pattern, params);
  }

  if (record.status === "pending") throw record.promise;
  if (record.status === "rejected") throw record.error;

  return { data: (record.result as Record<string, unknown>)?.[key] as T };
}

// --- invalidation ----------------------------------------------------------

function parseInvalidationPatterns(cookieValue: string): string[] {
  try {
    const decoded = decodeURIComponent(cookieValue);
    const parsed = JSON.parse(decoded);
    if (Array.isArray(parsed)) {
      return parsed;
    }
  } catch {
    // do nothing
  }
  return [cookieValue];
}

function matchesParameter(
  expectedValue: unknown,
  actualValue: unknown,
): boolean {
  if (typeof expectedValue === "string" && expectedValue.endsWith("*")) {
    const prefix = expectedValue.slice(0, -1);
    return typeof actualValue === "string" && actualValue.startsWith(prefix);
  }
  return actualValue === expectedValue;
}

function sweepCacheByPattern(
  patternBase: string,
  patternParams: Record<string, unknown>,
): void {
  for (const cacheKey of activeCache().keys()) {
    if (!cacheKey.startsWith(patternBase)) continue;

    const cacheFirstBrace = cacheKey.indexOf("{");
    if (cacheFirstBrace === -1) continue;

    try {
      const cacheParams = JSON.parse(cacheKey.slice(cacheFirstBrace));
      let isMatch = true;
      for (const [key, expectedValue] of Object.entries(patternParams)) {
        if (!matchesParameter(expectedValue, cacheParams[key])) {
          isMatch = false;
          break;
        }
      }
      if (isMatch) {
        activeCache().delete(cacheKey);
      }
    } catch {
      continue;
    }
  }
}

/**
 * Removes suspense-cache entries matching the given cache-key patterns.
 * Handles exact keys, base-pattern fallbacks, and wildcard parameter sweeps.
 */
export function invalidateCacheKeys(patterns: string[]): boolean {
  for (const pattern of patterns) {
    if (activeCache().has(pattern)) {
      activeCache().delete(pattern);
      continue;
    }

    const firstBrace = pattern.indexOf("{");

    if (firstBrace === -1) {
      const baseKey = pattern.replace(/:keys({.*})$/, "$1");
      if (baseKey !== pattern) {
        activeCache().delete(baseKey);
      }
      continue;
    }

    try {
      const patternBase = pattern.slice(0, firstBrace);
      const patternParams = JSON.parse(pattern.slice(firstBrace));
      const hasWildcard = Object.values(patternParams).some(
        (v) => typeof v === "string" && v.endsWith("*"),
      );
      if (hasWildcard) {
        sweepCacheByPattern(patternBase, patternParams);
      }
    } catch {
      // skip malformed patterns
    }
  }

  return true;
}

/**
 * Invalidates suspense-cache entries from a (legacy) redirect-driven
 * invalidation cookie payload. Used during SSR.
 */
export function invalidateCache(invalidateCacheCookie: string): boolean {
  return invalidateCacheKeys(parseInvalidationPatterns(invalidateCacheCookie));
}

// --- client-side reactivity -------------------------------------------------
// Framework-agnostic pub/sub so the React hook (react.ts) can react to
// invalidation/revalidation without a circular import. Listeners receive the
// cache keys that should be refreshed (or undefined for a blanket refresh).
type InvalidationListener = (cacheKeys?: string[]) => void;

const invalidationSubscribers = new Set<InvalidationListener>();

export function subscribeDataInvalidation(
  fn: InvalidationListener,
): () => void {
  invalidationSubscribers.add(fn);
  return () => {
    invalidationSubscribers.delete(fn);
  };
}

function emitDataInvalidation(cacheKeys?: string[]): void {
  invalidationSubscribers.forEach((fn) => fn(cacheKeys));
}

/**
 * Hard-clears client cache entries matching `patterns` then notifies. This
 * forces consumers to re-suspend, so prefer revalidatePageData for mutations.
 */
export function invalidatePageData(patterns?: string[]): void {
  if (patterns && patterns.length) {
    invalidateCacheKeys(patterns);
  }
  emitDataInvalidation();
}

/**
 * Notifies usePageData consumers to background-refetch the given cache keys in
 * place - stale data stays visible until fresh data arrives, so there is no
 * suspense flash. Use this after mutations.
 */
export function revalidatePageData(cacheKeys?: string[]): void {
  emitDataInvalidation(cacheKeys);
}

/**
 * Background-refetch a single cache entry via /__page-data and update it in
 * place, then call onUpdate so the consumer re-renders with the fresh data.
 */
export function refetchEntry(
  key: string,
  pattern: string,
  params: Record<string, unknown> | undefined,
  onUpdate: () => void,
): void {
  fetchPageDataRpc(pattern, params)
    .then((merged) => {
      if (!merged) return;
      const cacheKey = makeCacheKey(`${pattern}:${key}`, params);
      const record = activeCache().get(cacheKey);
      if (record) {
        record.status = "resolved";
        record.result = merged;
        record.timestamp = Date.now();
        record.promise = undefined;
        record.error = undefined;
      } else {
        activeCache().set(cacheKey, {
          status: "resolved",
          result: merged,
          timestamp: Date.now(),
        });
      }
      onUpdate();
    })
    .catch(() => {
      // keep stale data on refetch failure
    });
}

/**
 * Page-data registry interface.
 */
interface PageDataRegistry {
  registerPageDataLoader: (pageName: string, loader: PageDataLoader) => void;
  unregisterPageDataLoader: (pageName: string) => void;
  hasPageDataLoader: (pageName: string) => boolean;
  getRegisteredPageNames: () => string[];
  pageDataCache: PageDataCache;
}

export const pageDataRegistry: PageDataRegistry = {
  registerPageDataLoader,
  unregisterPageDataLoader,
  hasPageDataLoader,
  getRegisteredPageNames,
  pageDataCache,
};

/**
 * Registers a typed page-data loader.
 */
export function defineLoader<TData>(opts: {
  /**
   * Route pattern the loader serves.
   */
  pattern: string;
  /**
   * Loader returning the page's data slice, or null when absent.
   */
  loader: (
    params: Record<string, unknown>,
    context: PageDataContext,
  ) => Promise<TData | null>;
}): void {
  registerPageDataLoader(opts.pattern, async (params, context) => {
    const data = await opts.loader(params ?? {}, context ?? {});
    return data as Record<string, unknown> | null;
  });
}

export type { PageDataLoader };
