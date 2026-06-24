import type { ViteDevServer } from "vite";

/**
 * Loads a server module in development (via vite.ssrLoadModule) or production
 * (via dynamic import of the built bundle).
 *
 * @param vite     The Vite dev server, or undefined in production.
 * @param srcPath  Source path (e.g. "/src/entry-server.tsx").
 * @param distPath Optional built-module path for production (e.g. "../dist/server/entry-server.js").
 * @returns        The loaded module namespace.
 */
export async function loadModule(
  vite: ViteDevServer | undefined,
  srcPath: string,
  distPath?: string,
): Promise<Record<string, unknown>> {
  if (vite && typeof vite.ssrLoadModule === "function") {
    const abs = srcPath.startsWith("/")
      ? srcPath
      : srcPath.replace(/^\.?\.?\//, "/");
    try {
      return (await vite.ssrLoadModule(abs)) as Record<string, unknown>;
    } catch {
      try {
        return (await vite.ssrLoadModule(srcPath)) as Record<string, unknown>;
      } catch {
        // fall through to the production import
      }
    }
  }

  if (distPath) {
    return (await import(distPath)) as Record<string, unknown>;
  }
  return (await import(srcPath)) as Record<string, unknown>;
}
