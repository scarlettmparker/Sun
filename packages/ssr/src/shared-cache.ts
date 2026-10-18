/**
 * @fileoverview Cross-request TTL cache for cookie-independent data.
 */

type SharedEntry = {
  /**
   * Cached value
   */
  value?: unknown;
  /**
   * In-flight promise
   */
  pending?: Promise<unknown>;
  /**
   * Creation timestamp
   */
  timestamp: number;
};

const STORE_KEY = "__sun_sharedCache";

/**
 * @returns The process-wide store.
 */
function store(): Map<string, SharedEntry> {
  const g = globalThis as unknown as Record<
    string,
    Map<string, SharedEntry> | undefined
  >;
  return g[STORE_KEY] ?? (g[STORE_KEY] = new Map());
}

/**
 * Returns the cached value for a key, loading it on a miss. Concurrent misses
 * share one in-flight promise.
 *
 * @param key - Cache key.
 * @param ttlMs - Freshness window in ms.
 * @param load - Runs on a miss.
 * @returns The cached or freshly loaded value.
 */
export async function sharedCall<T>(
  key: string,
  ttlMs: number,
  load: () => Promise<T>,
): Promise<T> {
  const entries = store();
  const hit = entries.get(key);
  if (hit) {
    if (hit.pending) return hit.pending as Promise<T>;
    if (ttlMs > 0 && Date.now() - hit.timestamp < ttlMs) {
      return hit.value as T;
    }
  }

  const pending = (async () => {
    try {
      const value = await load();
      entries.set(key, { value, timestamp: Date.now() });
      return value;
    } catch (error) {
      entries.delete(key);
      throw error;
    }
  })();
  entries.set(key, { pending, timestamp: Date.now() });
  return pending;
}

/**
 * Drops entries matching the given keys, whole or by their param-block base.
 *
 * @param keys - Cache keys or key bases.
 */
export function invalidateSharedCache(keys: string[]): void {
  const entries = store();
  for (const key of keys) {
    const base = key.split("{")[0];
    for (const existing of [...entries.keys()]) {
      if (existing === key || existing.startsWith(base)) {
        entries.delete(existing);
      }
    }
  }
}

/**
 * Empties the shared cache.
 */
export function clearSharedCache(): void {
  store().clear();
}

/**
 * Freshness window
 */
export const SHARED_TTL = {
  /**
   * Config that only changes on deploy, such as theme and property sets.
   */
  CONFIG: 5 * 60_000,
  /**
   * A public list page.
   */
  LIST: 15_000,
  /**
   * A single public record.
   */
  RECORD: 30_000,
} as const;
