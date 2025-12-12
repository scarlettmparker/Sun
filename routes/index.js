/**
 * @fileoverview Defines and sets up all application routes.
 * @module routes
 */
import { renderApp } from "../utils/ssr.js";
import { base, isProduction } from "../config.js";
import { matchRoutes } from "react-router-dom";
import { routes } from "../src/router.tsx";
import { suspenseCache } from "../src/utils/page-data";

/**
 * Sets up all routes for the Express application.
 *
 * @param {express.Application} app - The Express application instance.
 * @param {object} vite - The Vite dev server instance (optional, only in development).
 */
export function setupRoutes(app, vite) {
  /**
   * Catch-all route for server-side rendering of pages.
   * This route handles all GET requests not otherwise handled by static file serving or specific API routes.
   * It fetches user data, loads translations, and renders the React application.
   * It also includes a basic check for file extensions to bypass SSR for static assets.
   *
   * @param {import("express").Request} req - Express request object.
   * @param {import("express").Response} res - Express response object.
   * @param {import("express").NextFunction} next - Express next middleware function.
   */
  app.get("*", async (req, res, next) => {
    const mutationPayloadCookie = req.cookies["mutation_payload"];
    const invalidateCacheCookie = req.cookies["invalidate_cache"];
    let mutationPayload = null;
    if (mutationPayloadCookie) {
      try {
        mutationPayload = JSON.parse(
          Buffer.from(mutationPayloadCookie, "base64").toString("utf-8")
        );
      } catch {}
    }

    // Skip SSR for requests with file extensions (e.g., .js, .css, .png)
    if (/\.[^/]+$/.test(req.path)) {
      return next();
    }

    let url = req.originalUrl.replace(base, "");
    if (!url.startsWith("/")) url = "/" + url;

    // Localization (Locale derivation is fine)
    const langHeader = req.headers["accept-language"] || "en";
    const locale = langHeader.split(",")[0] || "en";

    // Extract route params using React Router's matchRoutes
    const matches = matchRoutes(routes, url);
    const params = {};
    if (matches) {
      matches.forEach((match) => {
        Object.assign(params, match.params);
      });
    }

    try {
      await renderApp(
        {
          vite,
          isProduction,
          url,
          locale,
          mutationPayload,
          invalidateCacheCookie,
        },
        res
      );
    } catch (e) {
      console.error("Error during route handling:", e);
      res.status(500).end("Internal Server Error: " + e.message);
    }
  });
}
