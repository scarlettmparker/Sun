/**
 * Imports every module matched by an eager `import.meta.glob`, so colocated
 * `defineLoader`/`defineMutation` call sites register themselves at boot.
 */
export function autoDiscoverRegistrations(
  glob: Record<string, unknown>,
  label?: string,
): void {
  const count = Object.keys(glob).length;
  if (count > 0) {
    console.log(
      `[ssr] auto-loaded ${count} ${label ?? "registration"} module(s)`,
    );
  }
}
