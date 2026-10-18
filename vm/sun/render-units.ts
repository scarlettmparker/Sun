/**
 * Renders systemd user units for the local environment into ~/.config/systemd/user.
 */

import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { getManifest, type AppEntry, type BackendEntry } from "./apps";

const SUN_ROOT = path.resolve(import.meta.dir, "../..");
const UNIT_DIR = path.join(os.homedir(), ".config/systemd/user");
const BUN = path.join(os.homedir(), ".bun/bin/bun");
const JAVA = "/usr/bin/java";
const BACKEND_ENV = path.join(
  SUN_ROOT,
  "components/sun-graphql/src/main/resources/.env",
);
const ALLOWED_HOSTS = ".int.scarlettparker.co.uk,host.docker.internal,localhost";

/**
 * Builds the unit file body for a supervised app.
 */
function renderApp(app: AppEntry): string {
  const dir = path.resolve(SUN_ROOT, app.dir);
  return [
    "[Unit]",
    `Description=Sun app: ${app.name} (preview)`,
    "After=network-online.target sun-backend.service",
    "Wants=network-online.target",
    "StartLimitIntervalSec=60",
    "StartLimitBurst=5",
    "",
    "[Service]",
    "Type=simple",
    `WorkingDirectory=${dir}`,
    "Environment=NODE_ENV=production",
    `Environment=SERVER_PORT=${app.port}`,
    `Environment=VITE_SERVER_BASE=https://${app.host}`,
    `Environment=ALLOWED_HOSTS=${ALLOWED_HOSTS}`,
    `ExecStart=${BUN} --preload ./css-plugin.ts server.js`,
    "Restart=always",
    "RestartSec=3",
    "",
    "[Install]",
    "WantedBy=default.target",
    "",
  ].join("\n");
}

/**
 * Builds the unit file body for the backend.
 */
function renderBackend(backend: BackendEntry): string {
  const jar = path.isAbsolute(backend.jar)
    ? backend.jar
    : path.resolve(SUN_ROOT, backend.jar);
  const exec = [
    JAVA,
    ...(backend.jvmArgs ?? []),
    "-jar",
    jar,
    ...backend.args,
  ].join(" ");
  return [
    "[Unit]",
    "Description=Sun GraphQL Backend",
    "After=network-online.target",
    "Wants=network-online.target",
    "StartLimitIntervalSec=120",
    "StartLimitBurst=5",
    "",
    "[Service]",
    "Type=simple",
    `WorkingDirectory=${SUN_ROOT}`,
    `EnvironmentFile=${BACKEND_ENV}`,
    `ExecStart=${exec}`,
    "Restart=always",
    "RestartSec=5",
    "",
    "[Install]",
    "WantedBy=default.target",
    "",
  ].join("\n");
}

/**
 * Builds the unit file body for the reconcile oneshot.
 */
function renderReconcile(): string {
  return [
    "[Unit]",
    "Description=Sun preview reconcile",
    "",
    "[Service]",
    "Type=oneshot",
    `ExecStart=${BUN} ${path.join(SUN_ROOT, "vm/sun/sun-reconcile.ts")}`,
    "",
  ].join("\n");
}

/**
 * Builds the timer that drives the reconcile oneshot.
 */
function renderReconcileTimer(): string {
  return [
    "[Unit]",
    "Description=Sun preview reconcile timer",
    "",
    "[Timer]",
    "OnBootSec=30s",
    "OnUnitActiveSec=60s",
    "Unit=sun-reconcile.service",
    "",
    "[Install]",
    "WantedBy=timers.target",
    "",
  ].join("\n");
}

/**
 * Writes a unit file and returns its name.
 */
function writeUnit(name: string, contents: string): string {
  fs.writeFileSync(path.join(UNIT_DIR, name), contents);
  return name;
}

/**
 * Renders every enabled unit and prints their names.
 */
function main(): void {
  const manifest = getManifest();
  fs.mkdirSync(UNIT_DIR, { recursive: true });
  const units = [writeUnit(manifest.backend.unit, renderBackend(manifest.backend))];
  for (const app of manifest.apps) {
    if (app.enabled) {
      units.push(writeUnit(app.unit, renderApp(app)));
    }
  }
  writeUnit("sun-reconcile.service", renderReconcile());
  units.push(writeUnit("sun-reconcile.timer", renderReconcileTimer()));
  process.stdout.write(`${units.join("\n")}\n`);
}

main();
