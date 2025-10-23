/**
 * Deletes the window property from globalThis if it exists.
 * Used for testing server-side behavior by simulating environments without window.
 */
export function deleteGlobalWindow(): void {
  const g = globalThis;
  if ("window" in g) {
    Reflect.deleteProperty(g, "window");
  }
}
