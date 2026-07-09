/** localStorage key under which the active theme is persisted. */
export const THEME_STORAGE_KEY = "sun:theme";

/**
 * A theme's values keyed by property name. The set of valid keys and their
 * types is defined by the gaia property-set schema, not here.
 */
export type ThemeValues = Record<string, string>;
