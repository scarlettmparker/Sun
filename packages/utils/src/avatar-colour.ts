const DEFAULT_PRIMARY = "#d90429";

/**
 * Hash a string to a 32-bit integer (djb2).
 *
 * @param value string to hash
 * @returns unsigned 32-bit hash
 */
function hashStr(value: string): number {
  let hash = 5381;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 33) ^ value.charCodeAt(i);
  }
  return hash >>> 0;
}

/**
 * Derive a hue (0-359) from any string.
 *
 * @param value input to hash
 * @returns hue angle
 */
export function hashToHue(value: string): number {
  return hashStr(value) % 360;
}

/**
 * Convert a hex colour to HSL.
 *
 * @param hex hex colour including leading #
 * @returns HSL components or null when unparseable
 */
export function hexToHsl(
  hex: string,
): { h: number; s: number; l: number } | null {
  const stripped = hex.trim().replace(/^#/, "");
  const full =
    stripped.length === 3
      ? stripped
          .split("")
          .map((c) => c + c)
          .join("")
      : stripped;
  if (full.length !== 6 || !/^[0-9a-fA-F]{6}$/.test(full)) {
    return null;
  }
  const r = parseInt(full.substring(0, 2), 16) / 255;
  const g = parseInt(full.substring(2, 4), 16) / 255;
  const b = parseInt(full.substring(4, 6), 16) / 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const d = max - min;
  let h = 0;
  if (d !== 0) {
    if (max === r) {
      h = ((g - b) / d) % 6;
    } else if (max === g) {
      h = (b - r) / d + 2;
    } else {
      h = (r - g) / d + 4;
    }
    h *= 60;
    if (h < 0) {
      h += 360;
    }
  }
  const l = (max + min) / 2;
  const s = d === 0 ? 0 : d / (1 - Math.abs(2 * l - 1));
  return { h: Math.round(h), s: Math.round(s * 100), l: Math.round(l * 100) };
}

/**
 * Read a CSS variable from the document root.
 *
 * @param name custom property name including dashes
 * @returns trimmed value or undefined on server / when unset
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
 * Derive a seeded avatar colour from a username. Hue comes from the
 * username hash; saturation and lightness are taken from the current
 * --primary so the avatar stays in the same family as the theme.
 *
 * @param username seed for hue
 * @param fallbackHex hex used on server / when --primary unavailable
 * @returns hue, s, l and ready-to-use hsl background + contrasting foreground
 */
export function usernameToAvatarColour(
  username: string,
  fallbackHex: string = DEFAULT_PRIMARY,
): {
  hue: number;
  s: number;
  l: number;
  background: string;
  foreground: string;
} {
  const hue = hashToHue(username || "?");
  const primaryHex = readCssVar("--primary") ?? fallbackHex;
  const hsl = hexToHsl(primaryHex) ?? hexToHsl(DEFAULT_PRIMARY)!;
  const s = hsl.s;
  const l = hsl.l;
  const background = `hsl(${hue} ${s}% ${l}%)`;
  const foreground = l > 50 ? "#14141b" : "#ffffff";
  return { hue, s, l, background, foreground };
}

/**
 * Extract the display initial for a username.
 *
 * @param username raw username
 * @returns uppercased first character or "?"
 */
export function seedInitial(username: string): string {
  const trimmed = username.trim();
  if (!trimmed) {
    return "?";
  }
  return trimmed[0]!.toUpperCase();
}
