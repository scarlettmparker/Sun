const DAY_RGB = { r: 255, g: 250, b: 244 };
const NIGHT_RGB = { r: 253, g: 239, b: 242 };

/**
 * Reads a CSS custom property from the document root.
 *
 * @param name the custom property name, including the leading dashes
 * @returns the property value, or undefined when unavailable (e.g. on the server)
 */
function readCssVar(name: string): string | undefined {
  if (typeof window === "undefined") {
    return undefined;
  }
  const value = getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
  return value || undefined;
}

/**
 * Get the current second of the day. E.g. 12:00 returns 43,200
 *
 * @return Which second of the day it is.
 */
function getSecondsOfDay(): number {
  const dt = new Date();
  return dt.getSeconds() + 60 * (dt.getMinutes() + 60 * dt.getHours());
}

/**
 * Get the background colour for the current time of day.
 *
 * @returns The interpolated hex colour.
 */
export function getBackgroundHex(): string {
  const seconds = getSecondsOfDay();
  const ratio = seconds / 86400;

  const dayWeight = 1 - 2 * Math.abs(ratio - 0.5);

  const primary = DAY_RGB;
  const accent = NIGHT_RGB;

  const r = Math.round(primary.r * dayWeight + accent.r * (1 - dayWeight));
  const g = Math.round(primary.g * dayWeight + accent.g * (1 - dayWeight));
  const b = Math.round(primary.b * dayWeight + accent.b * (1 - dayWeight));

  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

/**
 * Convert HEX to rgb.
 * e.g. rgb("#FF0080") => { r : 256, g: 0, b: 128}
 *
 * @param hex Hex code to convert to rgb.
 * @returns The RGB object.
 */
function toRgb(hex: string) {
  const stripped = hex.replace("#", "");
  return {
    r: parseInt(stripped.substring(0, 2), 16),
    g: parseInt(stripped.substring(2, 4), 16),
    b: parseInt(stripped.substring(4, 6), 16),
  };
}

/**
 * Convert a channel value to a two-digit hex string.
 * e.g. hex(123) => "7b"
 *
 * @param value The 0-255 value to convert to hex.
 * @returns The hex string.
 */
function toHex(value: number) {
  const hex = value.toString(16);
  return hex.length == 1 ? "0" + hex : hex;
}
