import { THEME_STORAGE_KEY, type ThemeValues } from "./types";

/**
 * Event dispatched on window whenever a theme is applied.
 */
export const THEME_APPLIED_EVENT = "sun:theme-applied";

/**
 * Reads the persisted theme values, or an empty object when none or malformed.
 *
 * @returns the persisted theme values
 */
function readPersisted(): ThemeValues {
  if (typeof window === "undefined") {
    return {};
  }
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (!stored) {
    return {};
  }
  try {
    return JSON.parse(stored) as ThemeValues;
  } catch {
    return {};
  }
}

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

  // Remove any previously applied properties that are no longer present in the new theme.
  const nextKeys = new Set(Object.keys(values));
  for (const key of Object.keys(readPersisted())) {
    if (!nextKeys.has(key)) {
      root.style.removeProperty(`--${key}`);
    }
  }
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
  const values = readPersisted();
  if (Object.keys(values).length === 0) {
    return;
  }
  applyTheme(values);
}

/**
 * Clears any applied theme overrides and the persisted theme.
 */
export function clearTheme(): void {
  if (typeof window === "undefined") {
    return;
  }
  const root = document.documentElement;
  for (const key of Object.keys(readPersisted())) {
    root.style.removeProperty(`--${key}`);
  }
  window.localStorage.removeItem(THEME_STORAGE_KEY);
}
