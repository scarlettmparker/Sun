/**
 * Generic page data fetching utilities for SSR.
 * Provides a registry for page data loaders and a function to fetch data for any page.
 */

import { matchPath } from "react-router-dom";

type PageDataLoader = (
  params?: Record<string, unknown>
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
 * Cache for React Suspense-based data loading.
 * Stores the status of data fetches: 'pending', 'resolved', or 'rejected'.
 * Used by usePageData hook to suspend components until data is available.
 */
export const suspenseCache = new Map<
  string,
  {
    status: "pending" | "resolved" | "rejected";
    result?: Record<string, unknown>;
    promise?: Promise<unknown>;
    error?: unknown;
  }
>();

/**
 * Hydrates the page data cache with initial data from the server.
 * This populates the suspense cache with resolved data for each key in the initialData object.
 * It also handles normalized keys by adding a leading slash if missing and creates duplicate entries for normalized patterns.
 * After hydration, it notifies all registered listeners that the cache has been hydrated.
 *
 * @param initialData - An object containing initial page data keyed by cache keys, where each value is the data for that key.
 */
export function hydratePageData(
  initialData: Record<string, Record<string, unknown>>
) {
  if (!initialData) {
    return;
  }

  Object.keys(initialData).forEach((key) => {
    // Set the data in the suspense cache as resolved
    suspenseCache.set(key, { status: "resolved", result: initialData[key] });

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

// Cache expiration time in milliseconds (5 mins)
const CACHE_EXPIRATION_MS = 5 * 60 * 1000;

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
 * Normalizes a path string by removing the leading slash, if present.
 *
 * @param path The path string to normalize.
 * @returns The normalized path string.
 */
function normalizePath(path: string): string {
  let normalized = path.replace(/\/+$/, "");
  if (!normalized.startsWith("/")) {
    normalized = "/" + normalized;
  }
  return normalized;
}

/**
 * Invalidates the cache for a specific page pattern and parameters.
 *
 * @param pattern Route pattern (e.g., 'blog', 'blog/:id').
 * @param params Optional parameters that were used for the cached request.
 */
export function invalidateCache(
  pattern: string,
  params?: Record<string, unknown>
): void {
  const cacheKey = makeCacheKey(pattern, params || {});
  delete pageDataCache[cacheKey];
}

/**
 * Fetches data for a given URL path by finding the matching registered loader.
 * @param urlPath The actual URL path (e.g., 'blog/my-first-post').
 * @param explicitParams Optional manual parameters to override URL params.
 */
export async function fetchPageData(
  urlPath: string,
  explicitParams?: Record<string, unknown>
): Promise<Record<string, unknown> | null> {
  // Normalize the incoming URL path
  const normalizedUrlPath = normalizePath(urlPath);

  const registeredPatterns = Object.keys(pageDataLoaders);
  let matchedPattern: string | null = null;
  let urlParams: Record<string, string | undefined> = {};

  for (const pattern of registeredPatterns) {
    const normalizedPattern = normalizePath(pattern);

    const match = matchPath(
      { path: normalizedPattern, end: true },
      normalizedUrlPath
    );

    if (match) {
      matchedPattern = pattern;
      urlParams = match.params;
      break; // Stop at the first valid match
    }
  }

  if (!matchedPattern) {
    // If no match, check if the normalized URL is empty (root path)
    // and if a loader is registered for the empty string ("") or "/"
    if (
      !normalizedUrlPath &&
      registeredPatterns.some((p) => normalizePath(p) === "")
    ) {
      matchedPattern =
        registeredPatterns.find((p) => normalizePath(p) === "") || null;
    } else {
      return null;
    }
  }

  const loaders = pageDataLoaders[matchedPattern!];
  const finalParams = { ...urlParams, ...explicitParams };

  // Only use cache in production
  const isProduction = process.env.NODE_ENV === "production";
  const cacheKey = makeCacheKey(matchedPattern!, finalParams);

  if (isProduction) {
    const now = Date.now();
    const cached = pageDataCache[cacheKey];
    if (cached && now - cached.timestamp < CACHE_EXPIRATION_MS) {
      return cached.data;
    }
  }

  // Run all loaders.
  try {
    const results = await Promise.all(loaders.map((l) => l(finalParams)));
    const merged: Record<string, unknown> = {};
    let hasData = false;

    for (const r of results) {
      if (r && typeof r === "object") {
        Object.assign(merged, r);
        hasData = true;
      }
    }

    const data = hasData ? merged : null;

    // Store in suspenseCache so readPageData can find it
    suspenseCache.set(cacheKey, {
      status: "resolved",
      result: data || {},
    });

    // Only cache in production
    if (isProduction) {
      pageDataCache[cacheKey] = { data, timestamp: Date.now() };
    }

    return data;
  } catch (error) {
    console.error(`Failed to fetch data for pattern ${matchedPattern}:`, error);
    return null;
  }
}

/**
 * Creates a normalized cache key for the given pattern and params.
 * @param pattern The route pattern.
 * @param params Optional parameters.
 * @returns The cache key string.
 */
export function makeCacheKey(
  pattern: string,
  params?: Record<string, unknown>
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
  params?: Record<string, unknown>
): { data: T } {
  const cacheKey = makeCacheKey(pattern, params || {});

  let record = suspenseCache.get(cacheKey);

  if (!record) {
    // Find the loaders registered for this pattern
    const loaders = pageDataLoaders[pattern];
    if (!loaders || !loaders.length) {
      return { data: null as T };
    }

    // Initiate the data fetch
    const promise = Promise.all(loaders.map((l) => l(params)))
      .then((results) => {
        const merged: Record<string, unknown> = {};
        for (const r of results) {
          if (r && typeof r === "object") {
            Object.assign(merged, r);
          }
        }

        record!.status = "resolved";
        record!.result = merged;
        return merged[key];
      })
      .catch((err) => {
        console.error(`Error fetching page data for ${key} (${pattern}):`, err);
        record!.status = "rejected";
        record!.error = err;
      });

    record = { status: "pending", promise };
    suspenseCache.set(cacheKey, record);
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
export function usePageData<T>(
  key: string,
  pattern: string,
  params?: Record<string, unknown>
): { data: T } {
  const normalizedCacheKey = makeCacheKey(pattern, params);
  const rawCacheKey = `${pattern}:${JSON.stringify(params || {})}`;

  // Try normalized (leading slash) key first, then the raw key used during SSR.
  let record =
    suspenseCache.get(normalizedCacheKey) || suspenseCache.get(rawCacheKey);

  if (typeof window === "undefined" || !record) {
    return readPageData(key, pattern, params);
  }

  if (record!.status === "pending") throw record!.promise;
  if (record!.status === "rejected") throw record!.error;

  return { data: (record!.result as Record<string, unknown>)![key] as T };
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
  invalidateCache: (pattern: string, params?: Record<string, unknown>) => void;
}

export const pageDataRegistry: PageDataRegistry = {
  registerPageDataLoader,
  unregisterPageDataLoader,
  hasPageDataLoader,
  getRegisteredPageNames,
  pageDataCache,
  invalidateCache,
};
