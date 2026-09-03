/**
 * Resolved theme bundle for inlining and the client theme switcher.
 */
export type ResolvedTheme = {
  /**
   * Active theme values, inlined as CSS custom properties.
   */
  current: Record<string, string> | null;
  /**
   * Every available theme, exposed via window.__themes__.
   */
  all: { name: string; values: Record<string, string> }[];
};

/**
 * Parses a raw property set map into a resolved theme bundle.
 *
 * @param propertySet the raw gaiaQueries.propertySet value
 * @returns the resolved theme
 */
export function parseThemes(propertySet: unknown): ResolvedTheme {
  if (!propertySet || typeof propertySet !== "object") {
    return { current: null, all: [] };
  }
  const themeMap = propertySet as Record<string, Record<string, string>>;
  if (!Object.keys(themeMap).length) {
    return { current: null, all: [] };
  }
  return {
    current: themeMap["sea"] ?? Object.values(themeMap)[0] ?? null,
    all: Object.entries(themeMap).map(([name, values]) => ({
      name,
      values,
    })),
  };
}
