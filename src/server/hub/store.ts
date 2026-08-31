import { fetchHubRegistry, saveHubRegistry } from "~/utils/api";
import type { HubAppConfig, HubMode, HubRegistry } from "./types";
import {
  HubMode as GqlHubMode,
  type HubApp as GqlHubApp,
  type HubAppInput,
  type HubRegistry as GqlHubRegistry,
  type HubRegistryInput,
} from "~/generated/graphql";
import { DEFAULT_REGISTRY } from "./defaults";

const CACHE_TTL_MS = 30_000;

let cache: { registry: HubRegistry; at: number } | null = null;

/**
 * Maps a generated mode enum into the node mode.
 */
function toNodeMode(mode: GqlHubMode): HubMode {
  return mode === GqlHubMode.Serve ? "serve" : "dev";
}

/**
 * Maps the node mode into the generated mode enum.
 */
function toGqlMode(mode: HubMode): GqlHubMode {
  return mode === "serve" ? GqlHubMode.Serve : GqlHubMode.Dev;
}

/**
 * Maps a node registry into the generated input shape.
 */
function toGqlRegistry(registry: HubRegistry): HubRegistryInput {
  return {
    mode: toGqlMode(registry.mode),
    apps: registry.apps.map((app): HubAppInput => ({
      key: app.key,
      name: app.name,
      dir: app.dir,
      devPort: app.devPort,
      prodPort: app.prodPort,
      url: app.url,
      description: app.description,
      enabled: app.enabled,
      self: app.self === true,
    })),
  };
}

/**
 * Maps a generated registry into the node shape.
 */
function fromGqlRegistry(registry: GqlHubRegistry): HubRegistry {
  return {
    mode: toNodeMode(registry.mode),
    apps: registry.apps.map((app: GqlHubApp): HubAppConfig => ({
      key: app.key,
      name: app.name,
      dir: app.dir,
      devPort: app.devPort,
      prodPort: app.prodPort,
      url: app.url,
      description: app.description,
      enabled: app.enabled,
      self: app.self,
    })),
  };
}

/**
 * Resolves the first settled result, treating a timeout as null.
 */
async function withTimeout<T>(
  promise: Promise<T>,
  ms: number,
): Promise<T | null> {
  let timer: NodeJS.Timeout | undefined;
  const timeout = new Promise<null>((resolve) => {
    timer = setTimeout(() => resolve(null), ms);
  });
  try {
    return await Promise.race([promise, timeout]);
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Returns the current registry, falling back to the default when unreadable.
 */
export async function getRegistry(): Promise<HubRegistry> {
  if (cache && Date.now() - cache.at < CACHE_TTL_MS) {
    return cache.registry;
  }
  const result = await withTimeout(fetchHubRegistry(), 2_500);
  const stored = result?.success ? result.data?.gaiaQueries?.hubRegistry : null;
  const registry = stored ? fromGqlRegistry(stored) : DEFAULT_REGISTRY;
  cache = { registry, at: Date.now() };
  return registry;
}

/**
 * Persists the registry to the backend and refreshes the cache.
 *
 * @returns True when the write succeeded.
 */
export async function saveRegistry(registry: HubRegistry): Promise<boolean> {
  const result = await saveHubRegistry(toGqlRegistry(registry));
  if (!result.success) {
    return false;
  }
  cache = { registry, at: Date.now() };
  return true;
}
