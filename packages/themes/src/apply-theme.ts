import { THEME_STORAGE_KEY, type ThemeValues } from "./types";

/**
 * Event dispatched on window whenever a theme is applied.
 */
export const THEME_APPLIED_EVENT = "sun:theme-applied";

/**
 * Applies a theme by overriding the CSS custom properties on the document root,
 * one per value ("primary" becomes "--primary").
 *
 * @param values the theme values keyed by property name
 */
export function applyTheme(values: ThemeValues): void {
  if (typeof window === "undefined") {
    return;
  }
  const root = document.documentElement;
  for (const [key, value] of Object.entries(values)) {
    if (value) {
      root.style.setProperty(`--${key}`, value);
    }
  }
  window.localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(values));
  window.dispatchEvent(new Event(THEME_APPLIED_EVENT));
}

/**
 * Reapplies the persisted theme, if any.
 */
export function loadPersistedTheme(): void {
  if (typeof window === "undefined") {
    return;
  }
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (!stored) {
    return;
  }
  try {
    applyTheme(JSON.parse(stored) as ThemeValues);
  } catch {
    // Ignore malformed persisted themes.
  }
}

/**
 * Clears any applied theme overrides and the persisted theme.
 */
export function clearTheme(): void {
  if (typeof window === "undefined") {
    return;
  }
  const root = document.documentElement;
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (stored) {
    try {
      const values = JSON.parse(stored) as ThemeValues;
      for (const key of Object.keys(values)) {
        root.style.removeProperty(`--${key}`);
      }
    } catch {
      // Ignore malformed persisted themes.
    }
  }
  window.localStorage.removeItem(THEME_STORAGE_KEY);
}
