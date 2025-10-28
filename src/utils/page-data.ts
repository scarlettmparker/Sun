/**
 * Generic page data fetching utilities for SSR.
 * Provides a registry for page data loaders and a function to fetch data for any page.
 */

type PageDataLoader = () => Promise<Record<string, unknown> | null>;

/**
 * Registry of page data loaders.
 * Maps page names to their data loading functions.
 */
const pageDataLoaders: Record<string, PageDataLoader> = {};

/**
 * Registers a data loader for a specific page.
 * @param pageName The name of the page (e.g., 'stem-player').
 * @param loader Function that fetches data for the page.
 */
function registerPageDataLoader(
  pageName: string,
  loader: PageDataLoader
): void {
  pageDataLoaders[pageName] = loader;
}

/**
 * Unregisters a data loader for a specific page.
 * @param pageName The name of the page to unregister.
 */
function unregisterPageDataLoader(pageName: string): void {
  delete pageDataLoaders[pageName];
}

/**
 * Checks if a data loader is registered for a given page.
 * @param pageName The name of the page.
 * @returns True if a loader is registered, false otherwise.
 */
function hasPageDataLoader(pageName: string): boolean {
  return pageName in pageDataLoaders;
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
 * @returns Promise resolving to page data or null if no loader is registered.
 */
export async function fetchPageData(
  pageName: string
): Promise<Record<string, unknown> | null> {
  const loader = pageDataLoaders[pageName];
  if (!loader) {
    return null;
  }

  try {
    return await loader();
  } catch (error) {
    console.error(`Failed to fetch data for page ${pageName}:`, error);
    return null;
  }
}

// Export the registry functions as a single named export object for compliance with code style guide
export const pageDataRegistry = {
  registerPageDataLoader,
  unregisterPageDataLoader,
  hasPageDataLoader,
  getRegisteredPageNames,
};
