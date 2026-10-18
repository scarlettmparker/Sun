/**
 * Ecosystem app manifest, split by deployment environment.
 */

/**
 * A supervised frontend app.
 */
export type AppEntry = {
  /**
   * Stable identifier used in unit names and build hooks.
   */
  key: string;
  /**
   * Human-readable name.
   */
  name: string;
  /**
   * Hostname the reverse proxy routes to this app.
   */
  host: string;
  /**
   * Repo path relative to the Sun repo root.
   */
  dir: string;
  /**
   * Port the app listens on.
   */
  port: number;
  /**
   * systemd user unit that supervises the app.
   */
  unit: string;
  /**
   * Glob matching the active unit when the unit is blue-green.
   */
  unitPattern?: string;
  /**
   * Whether the environment supervises the app.
   */
  enabled: boolean;
};

/**
 * The GraphQL backend process.
 */
export type BackendEntry = {
  /**
   * systemd user unit that supervises the backend.
   */
  unit: string;
  /**
   * URL polled to check backend health.
   */
  health: string;
  /**
   * Jar path, relative to the Sun repo root or absolute.
   */
  jar: string;
  /**
   * Extra arguments passed to the jar.
   */
  args: string[];
};

/**
 * Everything the tooling and status page need for one environment.
 */
export type EnvironmentManifest = {
  /**
   * The backend process.
   */
  backend: BackendEntry;
  /**
   * Supervised frontend apps.
   */
  apps: AppEntry[];
};

const JAR = "components/sun-graphql/build/libs/sun-graphql-0.0.1-SNAPSHOT.jar";

/**
 * Environment selected by `SUN_ENV`, defaulting to local.
 */
export const ENVIRONMENT = process.env.SUN_ENV ?? "local";

/**
 * Manifests keyed by environment name.
 */
export const MANIFESTS: Record<string, EnvironmentManifest> = {
  local: {
    backend: {
      unit: "sun-backend.service",
      health: "http://localhost:8083/graphql",
      jar: JAR,
      args: ["--app.bypass-permissions=true"],
    },
    apps: [
      {
        key: "sun",
        name: "Sun",
        host: "sun.int.scarlettparker.co.uk",
        dir: ".",
        port: 5173,
        unit: "sun-app@sun.service",
        enabled: true,
      },
      {
        key: "filestore",
        name: "Filestore",
        host: "filestore.int.scarlettparker.co.uk",
        dir: "../Filestore",
        port: 5174,
        unit: "sun-app@filestore.service",
        enabled: true,
      },
      {
        key: "mame",
        name: "Emulator",
        host: "emulator.int.scarlettparker.co.uk",
        dir: "../MAME",
        port: 5175,
        unit: "sun-app@mame.service",
        enabled: true,
      },
      {
        key: "checklist",
        name: "Checklist",
        host: "checklist.int.scarlettparker.co.uk",
        dir: "../Checklist",
        port: 5176,
        unit: "sun-app@checklist.service",
        enabled: true,
      },
      {
        key: "viewer",
        name: "Viewer",
        host: "viewer.int.scarlettparker.co.uk",
        dir: "../Viewer",
        port: 5177,
        unit: "sun-app@viewer.service",
        enabled: true,
      },
      {
        key: "guided-reader",
        name: "Guided Reader",
        host: "reader.int.scarlettparker.co.uk",
        dir: "../Guided-Reader",
        port: 5178,
        unit: "sun-app@guided-reader.service",
        enabled: true,
      },
    ],
  },
  prod: {
    backend: {
      unit: "sun-backend.service",
      health: "http://localhost:8083/graphql",
      jar: "/opt/sun/backend/sun-graphql.jar",
      args: [],
    },
    apps: [
      {
        key: "sun",
        name: "Sun",
        host: "sun.scarlettparker.co.uk",
        dir: ".",
        port: 5173,
        unit: "sun-frontend@5173.service",
        unitPattern: "sun-frontend@*.service",
        enabled: true,
      },
      {
        key: "guided-reader",
        name: "Guided Reader",
        host: "reader.scarlettparker.co.uk",
        dir: ".",
        port: 5178,
        unit: "guided-reader@5178.service",
        unitPattern: "guided-reader@*.service",
        enabled: true,
      },
    ],
  },
};

/**
 * Returns the manifest for an environment, falling back to local.
 */
export function getManifest(
  environment: string = ENVIRONMENT,
): EnvironmentManifest {
  return MANIFESTS[environment] ?? MANIFESTS.local;
}
