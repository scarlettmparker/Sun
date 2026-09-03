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
      const result = await fetchPropertySet("ReactApp", "themes");
      const propertySet = result.success
        ? (result.data as { gaiaQueries?: { propertySet?: unknown } })
            ?.gaiaQueries?.propertySet
        : null;
      return { themes: parseThemes(propertySet) };
    } catch {
      return { themes: { current: null, all: [] } };
    }
  },
});
