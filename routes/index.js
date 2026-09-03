/**
 * @fileoverview Defines and sets up all application routes.
 * @module routes
 */
import { renderApp } from "@sun/ssr/server";
import { base, isProduction, manifestPath } from "../config.js";
import { Buffer } from "buffer";
import { getCookieValue } from "@sun/api";
import {
  AUTH_COOKIE,
  getCurrentUser,
  loginViaGaia,
  logoutViaBackend,
  buildAuthCookie,
  clearAuthCookie,
} from "../src/utils/auth.ts";
import { seedPageData } from "../src/utils/seed-page-data.ts";
import { registerHubRoutes } from "../src/server/hub/routes.ts";

const PUBLIC_PAGES = new Set(["/login", "/"]);

/**
 * Sets up routes for the Fastify application.
 *
 * @param {import("fastify").FastifyInstance} app - The Fastify application instance.
 * @param {object} vite - The Vite dev server instance (optional, only in development).
 */
export function setupRoutes(app, vite) {
  registerHubRoutes(app);

  app.post("/__login", async (request, reply) => {
    const { username, password } = request.body ?? {};
    const token = await loginViaGaia(username, password);
    if (!token) return reply.redirect("/login?error=1");
    reply.header("Set-Cookie", buildAuthCookie(token));
    return reply.redirect("/");
  });

  app.post("/__logout", async (request, reply) => {
    const token = getCookieValue(request.headers.cookie, AUTH_COOKIE);
    await logoutViaBackend(token);
    reply.header("Set-Cookie", clearAuthCookie());
    return reply.redirect("/login");
  });

  app.setNotFoundHandler({ method: ["GET"] }, async (request, reply) => {
    const requestUrl = new URL(request.raw.url, "http://localhost");
    const pathname = requestUrl.pathname;
    if (/\.[^/]+$/.test(pathname)) {
      return reply.callNotFound();
    }

    const normalizedPath =
      pathname.length > 1 && pathname.endsWith("/")
        ? pathname.replace(/\/+$/, "")
        : pathname;
    const isPublic = PUBLIC_PAGES.has(normalizedPath);

    const token = getCookieValue(request.headers.cookie, AUTH_COOKIE);
    let user = null;
    let isDeadSession = false;
    let isTransient = false;
    try {
      user = await getCurrentUser(token);
      isDeadSession = !user && !!token;
    } catch (_) {
      isTransient = !!token;
      isDeadSession = false;
    }

    if (isDeadSession) {
      reply.header("Set-Cookie", clearAuthCookie());
    }

    if (!user && !isPublic && !isTransient) return reply.redirect("/login");
    if (user && normalizedPath === "/login") return reply.redirect("/");

    await seedPageData(user);

    const mutationPayloadCookie = getCookieValue(
      request.headers.cookie,
      "mutation_payload",
    );
    const invalidateCacheCookie = getCookieValue(
      request.headers.cookie,
      "invalidate_cache",
    );
    let mutationPayload = null;
    if (mutationPayloadCookie) {
      try {
        mutationPayload = JSON.parse(
          Buffer.from(mutationPayloadCookie, "base64").toString("utf-8"),
        );
      } catch (_) {
        // Do nothing
      }
    }

    let url = pathname.replace(base, "");
    if (!url.startsWith("/")) url = "/" + url;
    if (requestUrl.search) url += requestUrl.search;

    const langHeader = request.headers["accept-language"] || "en";
    const locale = langHeader.split(",")[0] || "en";

    const pathOnly = url.split("?")[0];
    const pageName =
      pathOnly === "/" ? "home" : pathOnly.split("/")[1] || "home";

    try {
      await renderApp(
        {
          vite,
          isProduction,
          url,
          locale,
          pageName,
          mutationPayload,
          invalidateCacheCookie,
          manifestPath,
        },
        reply.raw,
      );
    } catch (e) {
      console.error("Error during route handling:", e);
      reply.status(500).send("Internal Server Error: " + e.message);
    }
  });
}
