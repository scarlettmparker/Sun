/**
 * Status types shared by the status endpoints and page.
 */

/**
 * Live state of one ecosystem app.
 */
export type AppStatus = {
  /**
   * Matching manifest key.
   */
  key: string;
  /**
   * Display name.
   */
  name: string;
  /**
   * Public hostname.
   */
  host: string;
  /**
   * Public URL.
   */
  url: string;
  /**
   * Listening port, or null when down.
   */
  port: number | null;
  /**
   * Whether the app is listening.
   */
  state: "up" | "down";
  /**
   * Epoch millis the supervising unit last started, or null.
   */
  since: number | null;
  /**
   * Server-formatted start time, or null.
   */
  sinceLabel: string | null;
  /**
   * Whether the environment supervises the app.
   */
  enabled: boolean;
};

/**
 * Live state of the backend.
 */
export type BackendStatus = {
  /**
   * Whether the backend unit is active or its port is open.
   */
  available: boolean;
  /**
   * Epoch millis the backend last started, or null.
   */
  since: number | null;
  /**
   * Server-formatted start time, or null.
   */
  sinceLabel: string | null;
};

/**
 * Full status snapshot for the running environment.
 */
export type StatusReport = {
  /**
   * Environment name from `SUN_ENV`.
   */
  environment: string;
  /**
   * Backend availability.
   */
  backend: BackendStatus;
  /**
   * Per-app status in manifest order.
   */
  apps: AppStatus[];
  /**
   * Epoch millis the snapshot was produced.
   */
  generatedAt: number;
};
