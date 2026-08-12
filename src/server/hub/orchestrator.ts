import { spawn } from "node:child_process";
import fs from "node:fs";
import net from "node:net";
import path from "node:path";
import type {
  AppRuntimeStatus,
  HubAppConfig,
  HubMode,
  HubRegistry,
  HubState,
} from "./types";

const DATA_DIR = path.resolve(process.cwd(), ".hub");
const STATE_FILE = path.join(DATA_DIR, "state.json");
const LOG_DIR = path.join(DATA_DIR, "logs");

const GRACE_MS = 5_000;

/**
 * Reads the persisted process state, defaulting to empty.
 */
function readState(): HubState {
  try {
    const raw = JSON.parse(fs.readFileSync(STATE_FILE, "utf-8")) as HubState;
    return { apps: raw.apps ?? {} };
  } catch {
    return { apps: {} };
  }
}

/**
 * Writes the process state to disk.
 */
function writeState(state: HubState): void {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(STATE_FILE, JSON.stringify(state, null, 2));
}

/**
 * Whether a process id is alive.
 */
function isPidAlive(pid: number): boolean {
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

/**
 * Whether a TCP port is listening.
 */
function isPortOpen(port: number): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = net.connect({ port, host: "127.0.0.1" });
    const done = (open: boolean): void => {
      socket.destroy();
      resolve(open);
    };
    socket.setTimeout(400);
    socket.once("connect", () => done(true));
    socket.once("timeout", () => done(false));
    socket.once("error", () => done(false));
  });
}

/**
 * Resolves an app's repo directory from the Sun repo root.
 */
function appDir(app: HubAppConfig): string {
  return path.resolve(process.cwd(), app.dir);
}

/**
 * Port an app should run on for the given mode.
 */
function portFor(app: HubAppConfig, mode: HubMode): number {
  return mode === "serve" ? app.prodPort : app.devPort;
}

/**
 * Terminates a detached process group, falling back to the process itself.
 */
function killGroup(pid: number, signal: NodeJS.Signals): void {
  try {
    process.kill(-pid, signal);
  } catch {
    try {
      process.kill(pid, signal);
    } catch {
      // already gone
    }
  }
}

/**
 * Spawns an app in the given mode, appending output to its log file.
 */
function spawnProcess(app: HubAppConfig, mode: HubMode): number | null {
  const port = portFor(app, mode);
  const script = mode === "serve" ? "preview" : "dev";
  const dir = appDir(app);
  if (!fs.existsSync(dir)) {
    return null;
  }
  fs.mkdirSync(LOG_DIR, { recursive: true });
  const logFd = fs.openSync(path.join(LOG_DIR, `${app.key}.log`), "a");
  const child = spawn("npm", ["run", script], {
    cwd: dir,
    detached: true,
    stdio: ["ignore", logFd, logFd],
    env: {
      ...process.env,
      SERVER_PORT: String(port),
      VITE_SERVER_BASE: app.url,
    },
  });
  child.on("error", () => undefined);
  child.unref();
  return child.pid ?? null;
}

let queue: Promise<unknown> = Promise.resolve();

/**
 * Runs a control operation exclusively, serialising spawn/stop races.
 */
function exclusive<T>(operation: () => Promise<T>): Promise<T> {
  const next = queue.then(operation);
  queue = next.catch(() => undefined);
  return next;
}

/**
 * Spawns an app when its port is free.
 */
async function startAppUnsafe(
  app: HubAppConfig,
  mode: HubMode,
): Promise<{ started: boolean; reason?: string; port: number }> {
  const port = portFor(app, mode);
  if (await isPortOpen(port)) {
    return { started: false, reason: "port-in-use", port };
  }
  const pid = spawnProcess(app, mode);
  if (pid === null) {
    return { started: false, reason: "spawn-failed", port };
  }
  const state = readState();
  state.apps[app.key] = { pid, port, mode, startedAt: Date.now() };
  writeState(state);
  return { started: true, port };
}

/**
 * Stops a managed app, waiting out the graceful window before SIGKILL.
 */
async function stopAppUnsafe(key: string): Promise<boolean> {
  const state = readState();
  const record = state.apps[key];
  if (!record) {
    return false;
  }
  killGroup(record.pid, "SIGTERM");
  const deadline = Date.now() + GRACE_MS;
  while (Date.now() < deadline && isPidAlive(record.pid)) {
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  if (isPidAlive(record.pid)) {
    killGroup(record.pid, "SIGKILL");
  }
  delete state.apps[key];
  writeState(state);
  return true;
}

/**
 * Spawns an app when its port is free.
 */
export function startApp(app: HubAppConfig, mode: HubMode) {
  return exclusive(() => startAppUnsafe(app, mode));
}

/**
 * Stops a managed app, waiting out the graceful window before SIGKILL.
 */
export function stopApp(key: string): Promise<boolean> {
  return exclusive(() => stopAppUnsafe(key));
}

/**
 * Stops then respawns an app in the given mode.
 */
export function restartApp(app: HubAppConfig, mode: HubMode) {
  return exclusive(async () => {
    await stopAppUnsafe(app.key);
    return startAppUnsafe(app, mode);
  });
}

/**
 * Live status of a single app.
 */
export async function statusOf(
  app: HubAppConfig,
  state: HubState,
): Promise<AppRuntimeStatus> {
  const record = state.apps[app.key];
  const managed = Boolean(record && isPidAlive(record.pid));
  const candidates = [record?.port, app.devPort, app.prodPort].filter(
    (port): port is number => typeof port === "number",
  );
  for (const port of [...new Set(candidates)]) {
    if (await isPortOpen(port)) {
      return {
        key: app.key,
        port,
        up: true,
        managed,
        external: !managed,
        pid: managed ? (record?.pid ?? null) : null,
      };
    }
  }
  return {
    key: app.key,
    port: null,
    up: false,
    managed,
    external: false,
    pid: managed ? (record?.pid ?? null) : null,
  };
}

/**
 * Live status of every app in the registry.
 */
export async function allStatuses(
  registry: HubRegistry,
): Promise<AppRuntimeStatus[]> {
  const state = readState();
  const statuses: AppRuntimeStatus[] = [];
  for (const app of registry.apps) {
    if (app.self) {
      statuses.push({
        key: app.key,
        port: app.devPort,
        up: true,
        managed: true,
        external: false,
        pid: null,
      });
      continue;
    }
    statuses.push(await statusOf(app, state));
  }
  return statuses;
}

/**
 * Spawns every enabled app whose port is free.
 */
export function reconcile(registry: HubRegistry) {
  return exclusive(async () => {
    const state = readState();
    const started: string[] = [];
    const skipped: string[] = [];
    for (const app of registry.apps) {
      if (!app.enabled || app.self) {
        continue;
      }
      const status = await statusOf(app, state);
      if (status.up) {
        skipped.push(app.key);
        continue;
      }
      const result = await startAppUnsafe(app, registry.mode);
      if (result.started) {
        started.push(app.key);
      } else {
        skipped.push(app.key);
      }
    }
    return { started, skipped };
  });
}

/**
 * Restarts every managed app in the given mode.
 *
 * @returns The apps restarted in the new mode.
 */
export function applyMode(registry: HubRegistry, mode: HubMode) {
  return exclusive(async () => {
    const state = readState();
    const restarted: string[] = [];
    for (const app of registry.apps) {
      if (app.self) {
        continue;
      }
      const record = state.apps[app.key];
      if (record && isPidAlive(record.pid)) {
        await stopAppUnsafe(app.key);
      }
      if (app.enabled) {
        const result = await startAppUnsafe(app, mode);
        if (result.started) {
          restarted.push(app.key);
        }
      }
    }
    return restarted;
  });
}
