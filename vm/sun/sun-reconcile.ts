/**
 * Starts any enabled app whose port is closed, recovering previews after a hard-killed dev session.
 */

import fs from "node:fs";
import net from "node:net";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { ENVIRONMENT, getManifest } from "./apps";

const STATE_DIR = path.join(os.homedir(), ".local/state/sun");

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
    socket.setTimeout(400);
    socket.once("connect", () => done(true));
    socket.once("timeout", () => done(false));
    socket.once("error", () => done(false));
  });
}

/**
 * Whether a live dev session owns the app, clearing a stale marker.
 */
function devActive(key: string): boolean {
  const marker = path.join(STATE_DIR, `dev-${key}`);
  let pid = NaN;
  try {
    pid = Number(fs.readFileSync(marker, "utf-8").trim());
  } catch {
    return false;
  }
  if (!Number.isInteger(pid) || pid <= 0) {
    fs.rmSync(marker, { force: true });
    return false;
  }
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    fs.rmSync(marker, { force: true });
    return false;
  }
}

/**
 * Starts a systemd user unit.
 */
function startUnit(unit: string): boolean {
  return (
    spawnSync("systemctl", ["--user", "start", unit], { stdio: "ignore" })
      .status === 0
  );
}

/**
 * Starts every enabled app whose port is closed and has no dev session.
 */
async function main(): Promise<void> {
  if (ENVIRONMENT !== "local") {
    return;
  }
  const started: string[] = [];
  const manifest = getManifest();
  for (const app of manifest.apps) {
    if (!app.enabled || devActive(app.key)) {
      continue;
    }
    if (await isPortOpen(app.port)) {
      continue;
    }
    if (startUnit(app.unit)) {
      started.push(app.key);
    }
  }
  for (const worker of manifest.workers) {
    if (!worker.enabled || devActive(worker.key)) {
      continue;
    }
    if (await isPortOpen(worker.port)) {
      continue;
    }
    if (startUnit(worker.unit)) {
      started.push(worker.key);
    }
  }
  if (started.length > 0) {
    console.log(`started: ${started.join(", ")}`);
  }
}

await main();
