/**
 * Imports every module matched by an eager `import.meta.glob`, so colocated
 * `defineLoader`/`defineMutation` call sites register themselves at boot.
 */
export function autoDiscoverRegistrations(
  _glob: Record<string, unknown>,
): void {
  // The eager glob already imported every module as a side effect; this hook
  // exists so the intent is explicit at the call site.
}
