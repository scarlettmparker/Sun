/**
 * Helper to load server-side modules in both dev and production.
 *
 * - In development when Vite server is available, prefer `vite.ssrLoadModule` so Vite
 *   transforms TypeScript/TSX files for Node.
 * - In production (no vite), fall back to importing the built server bundle (dist/server/entry-server.js)
 *   or an explicit distPath if provided.
 */
export async function loadModule(vite, srcPath, distPath) {
  // Try Vite SSR module loader first when available
  if (vite && typeof vite.ssrLoadModule === "function") {
    try {
      // vite expects absolute-ish paths like '/src/..'
      const abs = srcPath.startsWith("/")
        ? srcPath
        : srcPath.replace(/^\.?\.?\//, "/");
      return await vite.ssrLoadModule(abs);
    } catch (err) {
      // fallback to trying the original path if absolute failed
      try {
        return await vite.ssrLoadModule(srcPath);
      } catch (e) {
        // fall through to production path
        // console.warn("vite.ssrLoadModule failed for", srcPath, e);
      }
    }
  }

  // Production or fallback: try importing the built server bundle or provided distPath
  if (distPath) {
    return await import(distPath);
  }

  // Last-resort: try importing the source path directly (may fail for .ts/.tsx under plain Node)
  return await import(srcPath);
}

export default loadModule;
