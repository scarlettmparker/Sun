/**
 * Utility function for conditionally joining class names.
 * Filters out falsy values (undefined, null, false, empty strings) and joins with spaces.
 *
 * @param classes - Class names to join
 * @returns Joined class string
 */
export function cn(...classes: (string | undefined | null | false)[]): string {
  return classes.filter(Boolean).join(" ");
}
