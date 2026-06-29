import { FILESTORE_ORIGIN, EMULATOR_ORIGIN } from "../origins";

/**
 * Frontend mode constants representing which top-level app the user is
 * currently visiting. The same mode applies to that app AND anything it
 * embeds (e.g. Filestore iframed inside the Emulator reports EMULATOR).
 */
export const FrontendMode = {
  /**
   * Standalone Filestore app.
   */
  FILESTORE: "FILESTORE",
  /**
   * The MAME emulator
   */
  EMULATOR: "EMULATOR",
} as const;

export type FrontendMode = (typeof FrontendMode)[keyof typeof FrontendMode];

/**
 * Map of every known frontend to its origin. Used by both the client (origin matching) and the server (cookie
 * stamping). Add an entry here whenever a new frontend consumer is added.
 */
export const FRONTEND_ORIGINS: Record<FrontendMode, string> = {
  [FrontendMode.FILESTORE]: FILESTORE_ORIGIN,
  [FrontendMode.EMULATOR]: EMULATOR_ORIGIN,
};

/**
 * Name of the global into which the server renders the detected mode as
 * `<script>window.__FRONTEND_MODE__ = "EMULATOR"</script>` during SSR.
 */
export const FRONTEND_MODE_GLOBAL = "__FRONTEND_MODE__";

/**
 * Resolves an origin (or full URL) to its FrontendMode, or null if it is
 * not a known frontend. Accepts both `https://host` and `https://host/path`.
 */
export function matchOriginToMode(origin: string): FrontendMode | null {
  try {
    const normalized = new URL(origin).origin;
    for (const [mode, knownOrigin] of Object.entries(FRONTEND_ORIGINS)) {
      if (knownOrigin === normalized) {
        return mode as FrontendMode;
      }
    }
  } catch {
    // not a valid URL, no match
  }
  return null;
}

/**
 * Detects the frontend mode on the client.
 */
export function detectFrontendMode(): FrontendMode | null {
  if (typeof window !== "undefined") {
    const injected = (window as unknown as Record<string, unknown>)[
      FRONTEND_MODE_GLOBAL
    ];
    if (
      typeof injected === "string" &&
      Object.values(FrontendMode).includes(injected as FrontendMode)
    ) {
      return injected as FrontendMode;
    }
  }

  if (typeof window === "undefined") return null;

  const inIframe = (() => {
    try {
      return window.self !== window.top;
    } catch {
      // cross-origin access to window.top throws, we are in an iframe
      return true;
    }
  })();

  if (!inIframe) {
    return matchOriginToMode(window.location.origin);
  }
  return matchOriginToMode(document.referrer);
}
