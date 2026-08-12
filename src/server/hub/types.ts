/**
 * Shared hub registry and process types.
 */

/**
 * Runtime mode for hub-managed apps.
 */
export type HubMode = "dev" | "serve";

/**
 * Process control action for a hub app.
 */
export type ControlAction = "start" | "stop" | "restart";

/**
 * A hub app's settings.
 */
export type HubAppConfig = {
  /**
   * Unique identifier used for registry and control paths.
   */
  key: string;
  /**
   * Display name.
   */
  name: string;
  /**
   * Repo path relative to the Sun repo root.
   */
  dir: string;
  /**
   * Port used in dev mode.
   */
  devPort: number;
  /**
   * Port used in serve mode.
   */
  prodPort: number;
  /**
   * Public URL the hub page links to.
   */
  url: string;
  /**
   * One-line description.
   */
  description: string;
  /**
   * Whether the orchestrator manages this app.
   */
  enabled: boolean;
  /**
   * True for the Sun app itself, which is never spawned.
   */
  self?: boolean;
};

/**
 * The full hub registry stored in gaia.
 */
export type HubRegistry = {
  /**
   * Mode applied to spawned apps.
   */
  mode: HubMode;
  /**
   * Configured apps.
   */
  apps: HubAppConfig[];
};

/**
 * Live state of a hub app process.
 */
export type AppRuntimeStatus = {
  /**
   * Matching HubAppConfig key.
   */
  key: string;
  /**
   * Port found listening, or null when down.
   */
  port: number | null;
  /**
   * Whether the app's port is listening.
   */
  up: boolean;
  /**
   * Whether the process is tracked by the orchestrator.
   */
  managed: boolean;
  /**
   * Whether an unmanaged process holds the port.
   */
  external: boolean;
  /**
   * Managed process id, or null.
   */
  pid: number | null;
};

/**
 * Persisted orchestrator bookkeeping.
 */
export type HubState = {
  /**
   * Managed processes keyed by app key.
   */
  apps: Record<
    string,
    {
      /**
       * Process id of the spawned npm process.
       */
      pid: number;
      /**
       * Port the app was spawned on.
       */
      port: number;
      /**
       * Mode the app was spawned in.
       */
      mode: HubMode;
      /**
       * Epoch millis when the process was spawned.
       */
      startedAt: number;
    }
  >;
};
