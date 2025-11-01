/**
 * Generic page data fetching utilities for SSR.
 * Provides a registry for page data loaders and a function to fetch data for any page.
 */

type PageDataLoader = (
  params?: Record<string, unknown>
) => Promise<Record<string, unknown> | null>;

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
 */
const pageDataCache: PageDataCache = {};

// Cache expiration time in milliseconds (5 mins)
const CACHE_EXPIRATION_MS = 5 * 60 * 1000;

/**
 * Registers a data loader for a specific page.
 * @param pageName The name of the page (e.g., 'stem-player').
 * @param loader Function that fetches data for the page.
 */
function registerPageDataLoader(
  pageName: string,
  loader: PageDataLoader
): void {
  const list = pageDataLoaders[pageName] || [];
  // avoid duplicate registration of the same function
  if (!list.includes(loader)) {
    list.push(loader);
    pageDataLoaders[pageName] = list;

    // invalidate cache entries for this page
    Object.keys(pageDataCache).forEach((key) => {
      if (key === pageName || key.startsWith(`${pageName}:`)) {
        delete pageDataCache[key];
      }
    });
  }
}

/**
 * Unregisters a data loader for a specific page.
 * @param pageName The name of the page to unregister.
 */
/**
 * Unregisters all data loaders for a specific page.
 * If you need to unregister a single loader you can implement that later.
 */
function unregisterPageDataLoader(pageName: string): void {
  delete pageDataLoaders[pageName];
  Object.keys(pageDataCache).forEach((key) => {
    if (key === pageName || key.startsWith(`${pageName}:`)) {
      delete pageDataCache[key];
    }
  });
}

/**
 * Checks if a data loader is registered for a given page.
 * @param pageName The name of the page.
 * @returns True if a loader is registered, false otherwise.
 */
function hasPageDataLoader(pageName: string): boolean {
  const list = pageDataLoaders[pageName];
  return Array.isArray(list) && list.length > 0;
}

/**
 * Gets all registered page names.
 * @returns Array of registered page names.
 */
function getRegisteredPageNames(): string[] {
  return Object.keys(pageDataLoaders);
}

/**
 * Fetches data for a given page using the registered loader.
 * @param pageName The name of the page.
 * @param params Optional parameters to pass to the loader.
 * @returns Promise resolving to page data or null if no loader is registered.
 */
export async function fetchPageData(
  pageName: string,
  params?: Record<string, unknown>
): Promise<Record<string, unknown> | null> {
  const loaders = pageDataLoaders[pageName];
  if (!Array.isArray(loaders) || loaders.length === 0) {
    return null;
  }

  const cacheKey = params ? `${pageName}:${JSON.stringify(params)}` : pageName;
  const now = Date.now();
  const cached = pageDataCache[cacheKey];

  if (cached && now - cached.timestamp < CACHE_EXPIRATION_MS) {
    return cached.data;
  }

  // Run all loaders.
  try {
    const results = await Promise.all(loaders.map((l) => l(params)));
    const merged: Record<string, unknown> = {};
    let hasData = false;
    for (const r of results) {
      if (r && typeof r === "object") {
        Object.assign(merged, r);
        hasData = true;
      }
    }

    const data = hasData ? merged : null;
    pageDataCache[cacheKey] = { data, timestamp: now };
    return data;
  } catch (error) {
    console.error(`Failed to fetch data for page ${pageName}:`, error);
    return null;
  }
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
