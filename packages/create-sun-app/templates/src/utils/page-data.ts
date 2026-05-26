/**
 * Generic page data fetching utilities for SSR.
 * Provides a registry for page data loaders and a function to fetch data for any page.
 */

type PageDataLoader = (
  params?: Record<string, unknown>,
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

const FILESTORE_CACHE_TTL = 30000; // 30 seconds

/**
 * Cache TTL configuration in milliseconds.
 * Maps cache key patterns to their time-to-live values.
 * Default is 5 minutes (300000ms) if pattern not specified.
 */
const CACHE_TTL_MS: Record<string, number> = {
  "/bucket/:alias": FILESTORE_CACHE_TTL,
  "/bucket/:alias/:path": FILESTORE_CACHE_TTL,
};

const DEFAULT_CACHE_TTL_MS = 300000; // 5 minutes default

/**
 * Cache for React Suspense-based data loading.
 * Stores the status of data fetches: 'pending', 'resolved', or 'rejected'.
 * Used by getPageData to suspend components until data is available.
 */
export const suspenseCache = new Map<
  string,
  {
    status: "pending" | "resolved" | "rejected";
    result?: Record<string, unknown>;
    promise?: Promise<unknown>;
    error?: unknown;
    timestamp?: number;
  }
>();

/**
 * Gets the TTL for a given cache key pattern.
 * @param pattern The route pattern
 * @returns TTL in milliseconds
 */
function getCacheTTL(pattern: string): number {
  const normalized = pattern.startsWith("/") ? pattern : "/" + pattern;
  return CACHE_TTL_MS[normalized] ?? DEFAULT_CACHE_TTL_MS;
}

/**
 * Checks if a cache entry has expired.
 * @param record The cache record
 * @param pattern The route pattern
 * @returns True if expired, false otherwise
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
 * This populates the suspense cache with resolved data for each key in the initialData object.
 * It also handles normalized keys by adding a leading slash if missing and creates duplicate entries for normalized patterns.
 * After hydration, it notifies all registered listeners that the cache has been hydrated.
 *
 * @param initialData - An object containing initial page data keyed by cache keys, where each value is the data for that key.
 */
export function hydratePageData(
  initialData: Record<string, Record<string, unknown>>,
) {
  if (!initialData) {
    return;
  }

  Object.keys(initialData).forEach((key) => {
    // Set the data in the suspense cache as resolved with current timestamp
    suspenseCache.set(key, {
      status: "resolved",
      result: initialData[key],
      timestamp: Date.now(),
    });

    // Handle normalized keys: if the key contains a colon, normalize the pattern part
    const colonIndex = key.indexOf(":");
    if (colonIndex !== -1) {
      const patternPart = key.slice(0, colonIndex);
      const rest = key.slice(colonIndex);
      // Ensure the pattern starts with a slash
      const normalizedPattern = patternPart.startsWith("/")
        ? patternPart
        : "/" + patternPart;
      const normalizedKey = `${normalizedPattern}${rest}`;
      // Avoid overwriting existing normalized keys
      if (!suspenseCache.has(normalizedKey)) {
        suspenseCache.set(normalizedKey, {
          status: "resolved",
          result: initialData[key],
          timestamp: Date.now(),
        });
      }
    }
  });

  // Notify listeners that cache has been hydrated
  notifyCacheHydrated();
}

/**
 * Registry of page data loaders.
 * Maps page names to their data loading functions.
 */
// Allow multiple loaders per page name (e.g. layout + outlet)
export const pageDataLoaders: Record<string, PageDataLoader[]> = {};

type PageDataCache = Record<
  string,
  { data: Record<string, unknown> | null; timestamp: number }
>;

/**
 * Cache for page data to avoid re-fetching on every request.
 * Only used in production.
 */
const pageDataCache: PageDataCache = {};

/**
 * Registers a data loader for a specific page.
 * @param pattern The route pattern (e.g., 'blog', 'blog/:id').
 * @param loader Function that fetches data for the page.
 */
function registerPageDataLoader(pattern: string, loader: PageDataLoader): void {
  const list = pageDataLoaders[pattern] || [];

  // avoid duplicate registration of the same function
  if (!list.includes(loader)) {
    list.push(loader);
    pageDataLoaders[pattern] = list;

    // invalidate cache entries for this page
    Object.keys(pageDataCache).forEach((key) => {
      if (key.includes(pattern)) {
        delete pageDataCache[key];
      }
    });
  }
}

/**
 * Unregisters a data loader for a specific page.
 * @param pattern The route pattern to unregister.
 */
function unregisterPageDataLoader(pattern: string): void {
  delete pageDataLoaders[pattern];
  Object.keys(pageDataCache).forEach((key) => {
    if (key.includes(pattern)) {
      delete pageDataCache[key];
    }
  });
}

/**
 * Checks if a data loader is registered for a given page.
 * @param pageName The route pattern.
 * @returns True if a loader is registered, false otherwise.
 */
function hasPageDataLoader(pattern: string): boolean {
  return !!pageDataLoaders[pattern]?.length;
}

/**
 * Gets all registered page names.
 * @returns Array of registered page names.
 */
function getRegisteredPageNames(): string[] {
  return Object.keys(pageDataLoaders);
}

/**
 * Creates a normalized cache key for the given pattern and params.
 * @param pattern The route pattern.
 * @param params Optional parameters.
 * @returns The cache key string.
 */
export function makeCacheKey(
  pattern: string,
  params?: Record<string, unknown>,
) {
  const normalized = pattern.startsWith("/") ? pattern : "/" + pattern;
  return `${normalized}:${JSON.stringify(params || {})}`;
}

/**
 * Reads page data from the suspense cache, initiating fetch if not cached.
 * Used internally for suspense-based data loading.
 *
 * @param key The data key to retrieve.
 * @param pattern The route pattern.
 * @param params Optional parameters for the data loader.
 * @returns An object with the data, or throws a promise if pending.
 */
function readPageData<T>(
  key: string,
  pattern: string,
  params?: Record<string, unknown>,
): { data: T } {
  const cacheKey = makeCacheKey(`${pattern}:${key}`, params);
  let record = suspenseCache.get(cacheKey);

  // Check if cached record exists and has expired
  if (
    record &&
    record.status === "resolved" &&
    isCacheExpired(record, pattern)
  ) {
    suspenseCache.delete(cacheKey);
    record = undefined;
  }

  if (!record) {
    // Find the loaders registered for this pattern
    const loaders = pageDataLoaders[pattern];

    // Find loaders that return the specific key we need
    const relevantLoaders =
      loaders?.filter(() => {
        // We can't easily check what key a loader returns without calling it
        // So we'll call it and let it handle the logic
        return true;
      }) || [];

    if (!relevantLoaders.length) {
      return { data: null as T };
    }

    // Create record FIRST to ensure consistent reference in promise callbacks
    record = { status: "pending" };
    suspenseCache.set(cacheKey, record);

    // Initiate the data fetch
    const promise = Promise.all(relevantLoaders.map((l) => l(params)))
      .then((results) => {
        const merged: Record<string, unknown> = {};
        for (const r of results) {
          if (r && typeof r === "object") {
            Object.assign(merged, r);
          }
        }

        if (merged[key] == null) {
          record!.status = "rejected";
          record!.error = new Error(`No data returned for key: ${key}`);
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
        throw err;
      });

    record.promise = promise;
  }

  if (record!.status === "pending") {
    throw record!.promise;
  }
  if (record!.status === "rejected") {
    throw record!.error; // Error Boundary will catch this
  }

  // Return the resolved data
  return { data: record!.result![key] as T };
}

/**
 * Hook to read page data.
 *
 * @param key Data key to read (e.g., 'blogPosts')
 * @param pattern The route pattern (e.g., 'blog')
 * @param params API parameters
 */
export function getPageData<T>(
  key: string,
  pattern: string,
  params?: Record<string, unknown>,
): { data: T } {
  const cacheKey = makeCacheKey(`${pattern}:${key}`, params);
  let record = suspenseCache.get(cacheKey);

  // Check if cached record has expired
  if (
    record &&
    record.status === "resolved" &&
    isCacheExpired(record, pattern)
  ) {
    suspenseCache.delete(cacheKey);
    record = undefined;
  }

  // Fallback check to support key structures that were populated via hydratePageData
  if (!record) {
    const legacyHydrationKey = makeCacheKey(pattern, params);
    record = suspenseCache.get(legacyHydrationKey);

    // Check expiration on legacy key too
    if (
      record &&
      record.status === "resolved" &&
      isCacheExpired(record, pattern)
    ) {
      suspenseCache.delete(legacyHydrationKey);
      record = undefined;
    }
  }

  // If cache is invalidated or doesn't exist, execute/trigger fetch strategy
  if (typeof window === "undefined" || !record) {
    return readPageData(key, pattern, params);
  }

  if (record.status === "pending") throw record.promise;
  if (record.status === "rejected") throw record.error;

  return { data: (record.result as Record<string, unknown>)?.[key] as T };
}

/**
 * Parses a cache invalidation cookie value into an array of patterns.
 *
 * @param cookieValue The raw cookie value to parse.
 * @returns An array of string patterns to invalidate.
 */
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

/**
 * Checks if an actual cache parameter matches the expected pattern parameter.
 *
 * @param expectedValue The pattern value to match against (can include '*').
 * @param actualValue The actual value extracted from the cache key.
 * @returns True if the values match, false otherwise.
 */
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

/**
 * Sweeps the suspense cache and removes entries that match the given base pattern
 * and parameter conditions.
 *
 * @param patternBase Base string of the cache key (before the JSON params).
 * @param patternParams Parsed JSON parameters containing expected values.
 */
function sweepCacheByPattern(
  patternBase: string,
  patternParams: Record<string, unknown>,
): void {
  for (const cacheKey of suspenseCache.keys()) {
    if (!cacheKey.startsWith(patternBase)) continue;

    const cacheFirstBrace = cacheKey.indexOf("{");
    if (cacheFirstBrace === -1) continue;

    try {
      const cacheParams = JSON.parse(cacheKey.slice(cacheFirstBrace));
      let isMatch = true;

      // Ensure every parameter in the pattern matches the cache key's parameter
      for (const [key, expectedValue] of Object.entries(patternParams)) {
        if (!matchesParameter(expectedValue, cacheParams[key])) {
          isMatch = false;
          break;
        }
      }

      if (isMatch) {
        suspenseCache.delete(cacheKey);
      }
    } catch {
      // Skip cache keys with malformed JSON
      continue;
    }
  }
}

/**
 * Invalidates specific entries in the suspense cache based on a cookie payload.
 *
 * @param invalidateCacheCookie Raw cookie value containing invalidation patterns.
 */
export function invalidateCache(invalidateCacheCookie: string): boolean {
  const patterns = parseInvalidationPatterns(invalidateCacheCookie);

  for (const pattern of patterns) {
    // Handle exact cache key invalidation
    if (suspenseCache.has(pattern)) {
      suspenseCache.delete(pattern);
      continue;
    }

    const firstBrace = pattern.indexOf("{");

    // Standard base pattern fallback (No JSON parameters present)
    if (firstBrace === -1) {
      const baseKey = pattern.replace(/:keys({.*})$/, "$1");
      if (baseKey !== pattern) {
        suspenseCache.delete(baseKey);
      }
      continue;
    }

    // Handle parameter-based and wildcard invalidations
    try {
      const patternBase = pattern.slice(0, firstBrace);
      const patternParams = JSON.parse(pattern.slice(firstBrace));

      const hasWildcard = Object.values(patternParams).some(
        (v) => typeof v === "string" && v.endsWith("*"),
      );

      // Only execute sweep if a wildcard parameter was explicitly requested
      if (hasWildcard) {
        sweepCacheByPattern(patternBase, patternParams);
      }
    } catch (e) {}
  }

  return true;
}

/**
 * Page Data registry interface.
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
