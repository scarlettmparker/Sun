/**
 * @fileoverview Cross-request TTL cache for data that doesn't depend on the
 * caller.
 */

type SharedEntry = {
  /** Cached value */
  value?: unknown;
  /** In-flight promise */
  pending?: Promise<unknown>;
  /** Creation timestamp */
  timestamp: number;
};

const STORE_KEY = "__sun_sharedCache";

/**
 * The process-wide store. Kept on globalThis because @sun/ssr ships as separate
 * index/react/server bundles that each get their own module scope.
 *
 * @returns The shared map.
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
 * share one in-flight promise. Errors are not cached.
 *
 * @param key - Stable cache key.
 * @param ttlMs - Freshness window; 0 keeps only the in-flight sharing.
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
 * Drops entries matching the given keys. Each key is matched whole and by its
 * base (everything before the param block), so a pattern clears every variant.
 *
 * @param keys - Cache keys or key bases to drop.
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
 * Freshness windows for {@link sharedCall}, named by what the data is rather
 * than the duration, so call sites read as intent.
 */
export const SHARED_TTL = {
  /** Config that only changes on deploy, such as theme and property sets. */
  CONFIG: 5 * 60_000,
  /** A public list page. */
  LIST: 15_000,
  /** A single public record. */
  RECORD: 30_000,
} as const;
