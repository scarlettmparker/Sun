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
      const themesResult = await fetchPropertySet("ReactApp", "themes");
      if (themesResult.success) {
        propertySet = (
          themesResult.data as { gaiaQueries?: { propertySet?: unknown } }
        )?.gaiaQueries?.propertySet;
      }
      return { themes: parseThemes(propertySet) };
    } catch {
      return { themes: { current: null, all: [] } };
    }
  },
});
