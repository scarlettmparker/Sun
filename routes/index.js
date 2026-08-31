/**
 * @fileoverview Defines and sets up all application routes.
 * @module routes
 */
import { renderApp } from "@sun/ssr/server";
import { base, isProduction, manifestPath } from "../config.js";
import { Buffer } from "buffer";
import { createHmac, timingSafeEqual } from "crypto";
import { getCookieValue } from "@sun/api";
import {
  AUTH_COOKIE,
  loginViaGaia,
  buildAuthCookie,
  clearAuthCookie,
} from "../src/utils/auth.ts";
import { registerHubRoutes } from "../src/server/hub/routes.ts";

const PUBLIC_PAGES = new Set(["/login"]);

/**
 * Verifies a JWT's HMAC-SHA256 signature using the configured secret.
 * Returns the decoded payload if valid, null otherwise.
 */
function verifyToken(token) {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    try {
      const payload = JSON.parse(
        Buffer.from(token.split(".")[1], "base64url").toString(),
      );
      if (payload.exp && payload.exp * 1000 < Date.now()) return null;
      return payload;
    } catch {
      return {};
    }
  }

  const parts = token.split(".");
  if (parts.length !== 3) return null;

  const data = parts[0] + "." + parts[1];
  const expectedSig = createHmac("sha256", secret)
    .update(data)
    .digest("base64url");

  const expectedBuf = Buffer.from(expectedSig);
  const actualBuf = Buffer.from(parts[2]);
  if (
    expectedBuf.length !== actualBuf.length ||
    !timingSafeEqual(expectedBuf, actualBuf)
  ) {
    return null;
  }

  try {
    const payload = JSON.parse(Buffer.from(parts[1], "base64url").toString());
    if (payload.exp && payload.exp * 1000 < Date.now()) return null;
    return payload;
  } catch {
    return null;
  }
}

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
    const redirectTo =
      typeof request.query?.redirect === "string"
        ? request.query.redirect
        : "/";
    return reply.redirect(redirectTo);
  });

  app.post("/__logout", async (_request, reply) => {
    reply.header("Set-Cookie", clearAuthCookie());
    return reply.redirect("/login");
  });

  app.setNotFoundHandler({ method: ["GET"] }, async (request, reply) => {
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

    const requestUrl = new URL(request.raw.url, "http://localhost");
    const pathname = requestUrl.pathname;
    if (/\.[^/]+$/.test(pathname)) {
      return reply.callNotFound();
    }

    const token = getCookieValue(request.headers.cookie, AUTH_COOKIE);
    if (token) {
      const payload = verifyToken(token);
      if (!payload) {
        reply.header("Set-Cookie", clearAuthCookie());
        return reply.redirect(
          `/login?redirect=${encodeURIComponent(request.raw.url)}`,
        );
      }
    }

    const normalizedPath =
      pathname.length > 1 && pathname.endsWith("/")
        ? pathname.replace(/\/+$/, "")
        : pathname;
    const isPublic = PUBLIC_PAGES.has(normalizedPath);

    if (!token && !isPublic)
      return reply.redirect(
        `/login?redirect=${encodeURIComponent(request.raw.url)}`,
      );
    if (token && isPublic) return reply.redirect("/");

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
