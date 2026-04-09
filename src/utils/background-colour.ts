const DAY_RGB = toRgb("#FFF6ED");
const NIGHT_RGB = toRgb("#FEEBEF");

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
 * Get the background hex for the current time of the day. Uses 0.5 - ratio as
 * an absolute value so at mid day we see the day hex.
 */
export function getBackgroundHex(): string {
  const seconds = getSecondsOfDay();
  const ratio = seconds / 86400;

  // Get rgb values from hex ratio.
  const v = 1 - 2 * Math.abs(ratio - 0.5);

  const r = Math.round(DAY_RGB.r * v + NIGHT_RGB.r * (1 - v));
  const g = Math.round(DAY_RGB.g * v + NIGHT_RGB.g * (1 - v));
  const b = Math.round(DAY_RGB.b * v + NIGHT_RGB.b * (1 - v));

  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

/**
 * Convert HEX to rgb.
 * e.g. rgb("#FF0080") => { r : 256, g: 0, b: 128}
 *
 * @param hex Hex code to convert to rgb.
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
 * Convert RGB to hex.
 * e.g. hex(123) => "7b"
 *
 * @param rgb Rgb code to convert to hex.
 */
function toHex(rgb: number) {
  const hex = rgb.toString(16);
  return hex.length == 1 ? "0" + hex : hex;
}
