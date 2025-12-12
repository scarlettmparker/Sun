/**
 * Generic page data fetching utilities for SSR.
 * Provides a registry for page data loaders and a function to fetch data for any page.
 */

import { matchPath } from "react-router-dom";

type PageDataLoader = (
  params?: Record<string, unknown>
) => Promise<Record<string, unknown> | null>;

export const suspenseCache = new Map<
  string,
  { status: string; result?: any; promise?: Promise<any>; error?: any }
>();

/**
 * Registry of page data loaders.
 * Maps page names to their data loading functions.
 */
// Allow multiple loaders per page name (e.g. layout + outlet)
const pageDataLoaders: Record<string, PageDataLoader[]> = {};

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
/**
 * Unregisters all data loaders for a specific page.
 * If you need to unregister a single loader you can implement that later.
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
  const finalParams = params || {};
  const cacheKey = `${pattern}:${JSON.stringify(finalParams)}`;
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
  const cacheKey = `${matchedPattern}:${JSON.stringify(finalParams)}`;

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

export function makeCacheKey(
  pattern: string,
  params?: Record<string, unknown>
) {
  const normalized = pattern.startsWith("/") ? pattern : "/" + pattern;
  return `${normalized}:${JSON.stringify(params || {})}`;
}

function readPageData<T>(
  key: string,
  pattern: string,
  params?: Record<string, unknown>
): { data: T } {
  const cacheKey = `${pattern}:${JSON.stringify(params || {})}`;
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

  if (record.status === "pending") {
    throw record.promise;
  }
  if (record.status === "rejected") {
    throw record.error; // Error Boundary will catch this
  }

  // Return the resolved data
  return { data: record.result[key] as T };
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
  const complexCacheKey = makeCacheKey(pattern, params);
  let record = suspenseCache.get(complexCacheKey);

  if (typeof window === "undefined") {
    return readPageData(key, pattern, params);
  }

  if (!record) {
    try {
      return readPageData(key, pattern, params);
    } catch (promise) {
      throw promise;
    }
  }

  if (record.status === "pending") throw record.promise;
  if (record.status === "rejected") throw record.error;

  return { data: (record.result as Record<string, unknown>)[key] as T };
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
