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
    const parsed: unknown = JSON.parse(stored);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }
    return parsed as ThemeValues;
  } catch {
    return {};
  }
}

const SAFE_KEY_PATTERN = /^[A-Za-z0-9-]+$/;
const UNSAFE_VALUE_PATTERN = /url\s*\(|expression\s*\(|@import/i;
const MAX_VALUE_LENGTH = 256;

/**
 * Reports whether a persisted entry survives sanitising.
 *
 * @param key persisted property name
 * @param value persisted property value
 */
function isUsableEntry(key: string, value: unknown): value is string {
  return (
    SAFE_KEY_PATTERN.test(key) &&
    typeof value === "string" &&
    value.length <= MAX_VALUE_LENGTH &&
    !UNSAFE_VALUE_PATTERN.test(value)
  );
}

/**
 * Drops persisted entries with unsafe keys or values.
 */
function sanitizeValues(values: ThemeValues): ThemeValues {
  const clean: ThemeValues = {};
  for (const [key, value] of Object.entries(values)) {
    if (isUsableEntry(key, value)) {
      clean[key] = value;
    }
  }
  return clean;
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
  const safe = sanitizeValues(values ?? {});
  const root = document.documentElement;

  // Remove any previously applied properties that are no longer present in the new theme.
  const nextKeys = new Set(Object.keys(safe));
  for (const key of Object.keys(readPersisted())) {
    if (!nextKeys.has(key)) {
      root.style.removeProperty(`--${key}`);
    }
  }
  for (const [key, value] of Object.entries(safe)) {
    if (value) {
      root.style.setProperty(`--${key}`, value);
    }
  }
  window.localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(safe));
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
