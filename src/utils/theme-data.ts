import { defineLoader } from "@sun/ssr";
import { parseThemes, type ResolvedTheme } from "@sun/utils/property-set";
import { fetchPropertySet } from "./api";

/**
 * Loads available themes into the page-data cache non-blockingly.
 */
defineLoader({
  pattern: "themes",
  async loader(): Promise<{ themes: ResolvedTheme }> {
    try {
      let propertySet: unknown = null;
      const sunResult = await fetchPropertySet("ReactApp", "sun");
      if (sunResult.success) {
        propertySet = (sunResult.data as { gaiaQueries?: { propertySet?: unknown } })
          ?.gaiaQueries?.propertySet;
      }
      if (!propertySet || typeof propertySet !== "object" || !Object.keys(propertySet as object).length) {
        const fallback = await fetchPropertySet("ReactApp", "themes");
        if (fallback.success) {
          propertySet = (fallback.data as { gaiaQueries?: { propertySet?: unknown } })
            ?.gaiaQueries?.propertySet;
        }
      }
      return { themes: parseThemes(propertySet) };
    } catch {
      return { themes: { current: null, all: [] } };
    }
  },
});
