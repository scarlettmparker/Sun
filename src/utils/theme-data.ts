import { defineLoader, sharedCall, SHARED_TTL } from "@sun/ssr";
import { parseThemes, type ResolvedTheme } from "@sun/utils/property-set";
import { fetchPropertySet } from "./api";

/**
 * Loads available themes into the page-data cache non-blockingly.
 */
defineLoader({
  pattern: "themes",
  async loader(): Promise<{ themes: ResolvedTheme }> {
    const themes = await sharedCall(
      "shared:themes",
      SHARED_TTL.CONFIG,
      async () => {
        const themesResult = await fetchPropertySet("ReactApp", "themes");
        if (!themesResult.success) {
          throw new Error("themes unavailable");
        }
        const propertySet = (
          themesResult.data as { gaiaQueries?: { propertySet?: unknown } }
        )?.gaiaQueries?.propertySet;
        return parseThemes(propertySet);
      },
    ).catch(() => ({ current: null, all: [] }));
    return { themes };
  },
});
