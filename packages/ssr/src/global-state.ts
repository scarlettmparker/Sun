/**
 * Process-global singletons, so independently bundled copies of the framework
 * share one instance of each stateful object.
 */

/**
 * Returns the process-global value for a key, creating it on first use.
 *
 * @param key Stable identifier for the value.
 * @param create Factory that produces the value.
 * @returns The shared value.
 */
export function globalSingleton<T>(key: string, create: () => T): T {
  const symbol = Symbol.for(`sun.${key}`);
  const store = globalThis as unknown as Record<symbol, T | undefined>;
  let value = store[symbol];
  if (value === undefined) {
    value = create();
    store[symbol] = value;
  }
  return value;
}
