import { MAME_ORIGIN } from "../origins";

/**
 * Frontend mode constants representing which app is currently
 * consuming the Filestore UI.
 */
export const FrontendMode = {
  /**
   * Standalone Filestore app.
   */
  FILESTORE: "FILESTORE",
  /**
   * Iframed inside the MAME emulator.
   */
  EMULATOR: "EMULATOR",
} as const;

export type FrontendMode = (typeof FrontendMode)[keyof typeof FrontendMode];

/**
 * Detects the frontend mode based on whether the app is running inside
 * an iframe and, if so, which parent origin it came from.
 */
export function detectFrontendMode(): FrontendMode {
  if (typeof document === "undefined") return FrontendMode.FILESTORE;
  const isIframe = window.self !== window.top;
  if (isIframe && document.referrer) {
    try {
      const referrerOrigin = new URL(document.referrer).origin;
      if (referrerOrigin === MAME_ORIGIN) return FrontendMode.EMULATOR;
    } catch {
      // ignore parse errors
    }
  }
  return FrontendMode.FILESTORE;
}
