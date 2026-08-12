import { timingSafeEqual } from "node:crypto";
import { defineLoader, defineMutation, makeCacheKey } from "@sun/ssr";
import type { MutationResult } from "@sun/ssr";
import type { HubAppConfig, HubMode, HubRegistry } from "./types";
import { getRegistry, saveRegistry } from "./store";
import {
  startApp,
  stopApp,
  restartApp,
  reconcile,
  applyMode,
} from "./orchestrator";
import { emitStatusRescan } from "./status-events";

const MODES: HubMode[] = ["dev", "serve"];

const REGISTRY_CACHE_KEY = makeCacheKey("hub:hubRegistry", {});

/**
 * Successful mutation result, requesting a status rescan.
 */
function ok(message: string): MutationResult {
  emitStatusRescan();
  return {
    __typename: "QuerySuccess",
    message,
    invalidated: [REGISTRY_CACHE_KEY],
  };
}

/**
 * Failed mutation result.
 */
function fail(message: string): MutationResult {
  return { __typename: "StandardError", message };
}

/**
 * Whether the request holds the configured admin token.
 */
function authorized(body: Record<string, unknown>): boolean {
  const expectedToken = process.env.HUB_ADMIN_TOKEN ?? "";
  if (!expectedToken) {
    return true;
  }
  const submitted = typeof body.token === "string" ? body.token : "";
  const expected = Buffer.from(expectedToken);
  const actual = Buffer.from(submitted);
  return expected.length === actual.length && timingSafeEqual(expected, actual);
}

/**
 * Coerces an unknown body value into an app config, or null when malformed.
 */
function parseApp(value: unknown): HubAppConfig | null {
  if (!value || typeof value !== "object") {
    return null;
  }
  const raw = value as Record<string, unknown>;
  if (typeof raw.key !== "string" || !raw.key) {
    return null;
  }
  return {
    key: raw.key,
    name: typeof raw.name === "string" ? raw.name : raw.key,
    dir: typeof raw.dir === "string" ? raw.dir : `../${raw.key}`,
    devPort: Number(raw.devPort) || 5173,
    prodPort: Number(raw.prodPort) || 5173,
    url: typeof raw.url === "string" ? raw.url : "",
    description: typeof raw.description === "string" ? raw.description : "",
    enabled: raw.enabled !== false,
    self: raw.self === true,
  };
}

/**
 * Locates an app in the registry by key.
 */
function locateApp(registry: HubRegistry, key: string): HubAppConfig | null {
  return registry.apps.find((app) => app.key === key) ?? null;
}

/**
 * Whether the registry write failed and should be surfaced.
 */
async function persistOrFail(
  registry: HubRegistry,
): Promise<MutationResult | null> {
  const saved = await saveRegistry(registry);
  return saved ? null : fail("Failed to save registry");
}

/**
 * Loads the hub registry into the page-data cache.
 */
defineLoader({
  pattern: "hub",
  async loader() {
    return { hubRegistry: await getRegistry() };
  },
});

defineMutation({
  path: "hub/reconcile",
  async handler(body: Record<string, unknown>) {
    if (!authorized(body)) {
      return fail("Unauthorized");
    }
    const registry = await getRegistry();
    const result = await reconcile(registry);
    return ok(
      `Started ${result.started.length}, skipped ${result.skipped.length}`,
    );
  },
});

defineMutation({
  path: "hub/apps/create",
  async handler(body: Record<string, unknown>) {
    if (!authorized(body)) {
      return fail("Unauthorized");
    }
    const app = parseApp(body.app);
    if (!app) {
      return fail("Invalid app");
    }
    const registry = await getRegistry();
    if (locateApp(registry, app.key)) {
      return fail(`App already exists: ${app.key}`);
    }
    registry.apps.push(app);
    const failed = await persistOrFail(registry);
    if (failed) {
      return failed;
    }
    if (app.enabled && !app.self) {
      await startApp(app, registry.mode);
    }
    return ok(`Created ${app.key}`);
  },
});

defineMutation({
  path: "hub/apps/update",
  async handler(body: Record<string, unknown>) {
    if (!authorized(body)) {
      return fail("Unauthorized");
    }
    const key = typeof body.key === "string" ? body.key : "";
    const patch = parseApp(body.app);
    if (!patch || patch.key !== key) {
      return fail("Invalid app");
    }
    const registry = await getRegistry();
    const index = registry.apps.findIndex((app) => app.key === key);
    if (index < 0) {
      return fail(`Unknown app: ${key}`);
    }
    const previous = registry.apps[index];
    registry.apps[index] = patch;
    const failed = await persistOrFail(registry);
    if (failed) {
      return failed;
    }
    const runtimeChanged =
      previous.dir !== patch.dir ||
      previous.devPort !== patch.devPort ||
      previous.prodPort !== patch.prodPort;
    if (!patch.self && patch.enabled) {
      if (runtimeChanged) {
        await restartApp(patch, registry.mode);
      } else {
        await startApp(patch, registry.mode);
      }
    } else if (previous.enabled && !patch.enabled) {
      await stopApp(key);
    }
    return ok(`Updated ${key}`);
  },
});

defineMutation({
  path: "hub/apps/delete",
  async handler(body: Record<string, unknown>) {
    if (!authorized(body)) {
      return fail("Unauthorized");
    }
    const key = typeof body.key === "string" ? body.key : "";
    const registry = await getRegistry();
    const existing = locateApp(registry, key);
    if (!existing) {
      return fail(`Unknown app: ${key}`);
    }
    if (existing.self) {
      return fail("Cannot delete the current app");
    }
    registry.apps = registry.apps.filter((app) => app.key !== key);
    const failed = await persistOrFail(registry);
    if (failed) {
      return failed;
    }
    await stopApp(key);
    return ok(`Deleted ${key}`);
  },
});

defineMutation({
  path: "hub/apps/start",
  async handler(body: Record<string, unknown>) {
    if (!authorized(body)) {
      return fail("Unauthorized");
    }
    const key = typeof body.key === "string" ? body.key : "";
    const registry = await getRegistry();
    const app = locateApp(registry, key);
    if (!app) {
      return fail(`Unknown app: ${key}`);
    }
    const result = await startApp(app, registry.mode);
    return result.started
      ? ok(`Started ${key}`)
      : fail(`Could not start ${key} (${result.reason})`);
  },
});

defineMutation({
  path: "hub/apps/stop",
  async handler(body: Record<string, unknown>) {
    if (!authorized(body)) {
      return fail("Unauthorized");
    }
    const key = typeof body.key === "string" ? body.key : "";
    const registry = await getRegistry();
    const app = locateApp(registry, key);
    if (!app) {
      return fail(`Unknown app: ${key}`);
    }
    if (app.self) {
      return fail("Cannot stop the current app");
    }
    const stopped = await stopApp(key);
    return stopped ? ok(`Stopped ${key}`) : fail(`Not managed: ${key}`);
  },
});

defineMutation({
  path: "hub/apps/restart",
  async handler(body: Record<string, unknown>) {
    if (!authorized(body)) {
      return fail("Unauthorized");
    }
    const key = typeof body.key === "string" ? body.key : "";
    const registry = await getRegistry();
    const app = locateApp(registry, key);
    if (!app) {
      return fail(`Unknown app: ${key}`);
    }
    if (app.self) {
      return fail("Cannot restart the current app");
    }
    const result = await restartApp(app, registry.mode);
    return result.started
      ? ok(`Restarted ${key}`)
      : fail(`Could not restart ${key} (${result.reason})`);
  },
});

defineMutation({
  path: "hub/mode",
  async handler(body: Record<string, unknown>) {
    if (!authorized(body)) {
      return fail("Unauthorized");
    }
    const mode = body.mode as HubMode;
    if (!MODES.includes(mode)) {
      return fail("Invalid mode");
    }
    const registry = await getRegistry();
    registry.mode = mode;
    const failed = await persistOrFail(registry);
    if (failed) {
      return failed;
    }
    const restarted = await applyMode(registry, mode);
    return ok(`Mode set to ${mode}, restarted ${restarted.length}`);
  },
});
