import net from "node:net";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { ENVIRONMENT, getManifest, type AppEntry } from "../../../vm/sun/apps.ts";
import type { AppStatus, StatusReport } from "./types";

const execFileAsync = promisify(execFile);

/**
 * Whether a TCP port is accepting connections.
 */
function isPortOpen(port: number): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = net.connect({ port, host: "127.0.0.1" });
    const done = (open: boolean): void => {
      socket.destroy();
      resolve(open);
    };
    socket.setTimeout(300);
    socket.once("connect", () => done(true));
    socket.once("timeout", () => done(false));
    socket.once("error", () => done(false));
  });
}

/**
 * Parses epoch millis from a systemd local-time timestamp.
 */
function parseTimestamp(value: string): number | null {
  const match = /^[A-Za-z]+ (\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})/.exec(value);
  if (!match) {
    return null;
  }
  return new Date(
    Number(match[1]),
    Number(match[2]) - 1,
    Number(match[3]),
    Number(match[4]),
    Number(match[5]),
    Number(match[6]),
  ).getTime();
}

/**
 * Formats epoch millis as a local `YYYY-MM-DD HH:mm:ss` label.
 */
function formatSince(ms: number | null): string | null {
  if (ms == null) {
    return null;
  }
  const date = new Date(ms);
  const pad = (value: number): string => String(value).padStart(2, "0");
  return [
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`,
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`,
  ].join(" ");
}

/**
 * Reads a unit's active state and start time.
 */
async function unitState(unit: string): Promise<{ active: boolean; since: number | null }> {
  try {
    const { stdout } = await execFileAsync("systemctl", [
      "--user",
      "show",
      unit,
      "-p",
      "ActiveState",
      "-p",
      "ActiveEnterTimestamp",
    ]);
    const fields = new Map(
      stdout
        .trim()
        .split("\n")
        .map((line) => {
          const index = line.indexOf("=");
          return [line.slice(0, index), line.slice(index + 1)];
        }),
    );
    const active = fields.get("ActiveState") === "active";
    const raw = fields.get("ActiveEnterTimestamp") ?? "";
    return { active, since: active ? parseTimestamp(raw) : null };
  } catch {
    return { active: false, since: null };
  }
}

/**
 * Resolves the live unit for an app, following a blue-green pattern, and the
 * port that instance listens on.
 */
async function resolveUnit(app: AppEntry): Promise<{ unit: string; port: number }> {
  if (!app.unitPattern) {
    return { unit: app.unit, port: app.port };
  }
  try {
    const { stdout } = await execFileAsync("systemctl", [
      "--user",
      "list-units",
      "--all",
      "--plain",
      "--no-legend",
      app.unitPattern,
    ]);
    for (const line of stdout.trim().split("\n")) {
      const [unit, , state] = line.trim().split(/\s+/);
      if (unit && state === "active") {
        const match = /@(\d+)/.exec(unit);
        return { unit, port: match ? Number(match[1]) : app.port };
      }
    }
  } catch {
    return { unit: app.unit, port: app.port };
  }
  return { unit: app.unit, port: app.port };
}

/**
 * Ports parsed from a URL, defaulting to the scheme's default port.
 */
function portFromUrl(url: string): number | null {
  try {
    const parsed = new URL(url);
    if (parsed.port) {
      return Number(parsed.port);
    }
    return parsed.protocol === "https:" ? 443 : 80;
  } catch {
    return null;
  }
}

/**
 * Builds the full status snapshot for the running environment.
 */
export async function buildReport(): Promise<StatusReport> {
  const manifest = getManifest();
  const apps = await Promise.all(
    manifest.apps.map(async (app): Promise<AppStatus> => {
      const { unit, port } = await resolveUnit(app);
      const [open, state] = await Promise.all([isPortOpen(port), unitState(unit)]);
      return {
        key: app.key,
        name: app.name,
        host: app.host,
        url: `https://${app.host}`,
        port: open ? port : null,
        state: open ? "up" : "down",
        since: state.since,
        sinceLabel: formatSince(state.since),
        enabled: app.enabled,
      };
    }),
  );

  const backend = await unitState(manifest.backend.unit);
  const backendPort = portFromUrl(manifest.backend.health);
  const backendOpen = backendPort !== null && (await isPortOpen(backendPort));

  return {
    environment: ENVIRONMENT,
    backend: {
      available: backend.active || backendOpen,
      since: backend.since,
      sinceLabel: formatSince(backend.since),
    },
    apps,
    generatedAt: Date.now(),
  };
}
