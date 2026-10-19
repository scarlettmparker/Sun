import path from "path";
import type { IncomingMessage, ServerResponse } from "node:http";
import type { ViteDevServer } from "vite";
import type { FastifyInstance } from "fastify";
import { pageDataRpcHandler } from "./rpc-handler";
import { mutationRegistry } from "./mutations";
import { MutationError } from "./mutation-error";
import { registerSecurity } from "./security";
import { shutdownAnalytics, trackPageView } from "@sun/analytics/server";
import { registerIpWhitelist, type IpWhitelistConfig } from "./ip-whitelist";
import {
  buildSecurityHeaders,
  buildHstsHeader,
  buildCoopHeader,
  buildCoepHeader,
  buildCorpHeader,
} from "@sun/security";
import { enterCspNonce, getCspNonce } from "./csp-nonce";
import { enterRequestContext } from "./request-context";
import { fastifyApp } from "./fastify-adapter";
import { startBunServer } from "./bun-server";
import type { SunApp, SunReply, SunRequest } from "./server-types";
export type { SunApp, SunReply, SunRequest } from "./server-types";

export { handleQuery } from "./query";
export type { QueryFetchResult } from "./query";
export { loadModule } from "./load-module";
export { renderApp } from "./render-stream";
export type { RenderAppOptions } from "./render-stream";
export type {
  AppRenderConfig,
  RenderOptions,
  RenderResult,
  ResolvedTheme,
  RouteMeta,
} from "./renderer";
export { autoDiscoverRegistrations } from "./auto-discover";

type ServerConfig = {
  port: number;
  host: string;
  base: string;
  isProduction: boolean;
  backendHost: string;
  backendPort: number;
  /**
   * HMAC key for CSRF tokens. CSRF protection is disabled when unset.
   */
  clientSecret?: string;
  /**
   * Hostname suffixes allowed by the origin gate.
   */
  allowedOrigins?: string[];
  /**
   * App identifier forwarded to the backend.
   */
  clientId?: string;
  /**
   * Enables the IP whitelist gate, refreshed from the backend.
   */
  ipWhitelist?: IpWhitelistConfig;
};

/**
 * Registers the app's SSR catch-all and any app-specific GET routes.
 *
 * @param app  The server surface, already wired with dev/prod middleware.
 * @param vite The Vite dev server in development, or undefined in production.
 * @returns    void, or a Promise that resolves once routes are registered.
 */
type SetupRoutes = (app: SunApp, vite: ViteDevServer | undefined) => void | Promise<void>;

/**
 * Optional hook for app-specific middleware or custom routes. Runs before the
 * page-data route, the app routes, and the mutation catch-all.
 *
 * @param app  The server surface.
 * @param vite The Vite dev server in development, or undefined in production.
 * @returns    void, or a Promise that resolves once configuration is done.
 */
type ConfigureServer = (app: SunApp, vite: ViteDevServer | undefined) => void | Promise<void>;

interface CreateServerOptions {
  /**
   * Env-driven server config from the app's config.js.
   */
  config: ServerConfig;
  /**
   * Registers the SSR catch-all and any app-specific GET routes.
   */
  setupRoutes: SetupRoutes;
  /**
   * Optional pre-route middleware or custom-route hook.
   */
  configure?: ConfigureServer;
  /**
   * Enables server-side PostHog capture plus the /__analytics beacon.
   */
  analytics?: {
    /**
     * App name reported on every event.
     */
    app: string;
    /**
     * Cookie holding the auth JWT, used to attribute events to an account.
     */
    authCookie?: string;
    /**
     * Consent cookie that must read accepted before capturing.
     */
    consentCookie?: string;
  };
}

type MiddieHandler = (req: IncomingMessage, res: ServerResponse) => void;

/**
 * Registers security headers, CSRF, the page-data RPC, analytics, the app
 * routes, and the mutation catch-all on a server surface.
 *
 * @param app     The server surface.
 * @param options Server config and app callbacks.
 * @param vite    The Vite dev server in development.
 * @returns       A promise that resolves once registration is complete.
 */
async function registerApp(
  app: SunApp,
  options: CreateServerOptions,
  vite: ViteDevServer | undefined,
): Promise<void> {
  const { config, setupRoutes, configure } = options;

  app.addHook("onSend", async (request: SunRequest, reply: SunReply, payload) => {
    const contentType = String(reply.getHeader("content-type") ?? "");
    const isHtml =
      contentType.includes("text/html") ||
      (typeof payload === "string" && payload.includes("<!DOCTYPE html>")) ||
      request.url === "/" ||
      request.url.startsWith("/?");

    const nonce = getCspNonce();
    if (nonce && isHtml) {
      const headers = buildSecurityHeaders({
        isProduction: config.isProduction,
        nonce,
      });
      for (const [key, value] of Object.entries(headers)) {
        if (key === "Content-Security-Policy" && reply.getHeader(key)) {
          continue;
        }
        reply.header(key, value);
      }
      return;
    }

    if (config.isProduction) {
      reply.header("Strict-Transport-Security", buildHstsHeader());
    }
    reply.header("Cross-Origin-Opener-Policy", buildCoopHeader(false));
    reply.header("Cross-Origin-Embedder-Policy", buildCoepHeader(true));
    reply.header("Cross-Origin-Resource-Policy", buildCorpHeader("same-site"));
    reply.header("X-Content-Type-Options", "nosniff");
    reply.header("X-Frame-Options", "DENY");
    reply.header("Referrer-Policy", "strict-origin-when-cross-origin");
    reply.header("Permissions-Policy", "geolocation=(), camera=(), microphone=(), payment=()");
  });

  if (configure) {
    await configure(app, vite);
  }

  registerSecurity(app, {
    clientSecret: config.clientSecret,
    allowedOrigins: config.allowedOrigins,
    isProduction: config.isProduction,
  });

  if (config.ipWhitelist) {
    registerIpWhitelist(app, config.ipWhitelist, config.clientId);
  }

  app.post("/__page-data", pageDataRpcHandler());

  if (options.analytics) {
    app.post("/__analytics", analyticsBeaconHandler(options.analytics));
  }

  await setupRoutes(app, vite);

  app.route({
    method: "POST",
    url: "/*",
    handler: mutationPostHandler(),
  });
}

/**
 * Starts the Fastify development server with Vite middleware.
 *
 * @param options Server config and app callbacks.
 * @returns The bound address.
 */
async function startDevServer(options: CreateServerOptions): Promise<string> {
  const { config } = options;
  const { default: Fastify } = await import("fastify");
  const { default: fastifyStatic } = await import("@fastify/static");

  const fastify: FastifyInstance = Fastify({ logger: false, trustProxy: true });

  await fastify.register(fastifyStatic, {
    root: path.resolve("./public"),
    prefix: "/",
    decorateReply: false,
  });

  const { default: fastifyMiddie } = await import("@fastify/middie");
  await fastify.register(fastifyMiddie);

  const { createServer: createViteServer } = await import("vite");
  const vite = await createViteServer({
    server: { middlewareMode: true },
    appType: "custom",
    base: config.base,
  });
  fastify.use(vite.middlewares as unknown as MiddieHandler);

  const app = fastifyApp(fastify);
  app.addHook("onRequest", (request) => {
    enterRequestContext({ cookie: request.headers.cookie, ip: request.ip });
    enterCspNonce();
  });

  await registerApp(app, options, vite);

  const address = await fastify.listen({
    port: config.port,
    host: config.host,
  });
  console.log(`Server started at ${address}`);

  let shuttingDown = false;

  /**
   * Drains in-flight requests before exiting.
   *
   * @param reason The signal that triggered shutdown.
   * @param done   Optional continuation that runs instead of process exit.
   */
  const gracefulShutdown = async (reason: string, done?: () => void) => {
    if (shuttingDown) return;
    shuttingDown = true;
    console.log(`${reason} received, draining in-flight requests`);

    const force = setTimeout(() => {
      console.error("Force-exiting after shutdown timeout");
      process.exit(1);
    }, 10_000);
    force.unref();

    try {
      await fastify.close();
      await shutdownAnalytics();
    } catch (err) {
      console.error("Error during graceful shutdown:", err);
    }

    if (done) {
      done();
    } else {
      process.exit(0);
    }
  };

  process.once("SIGUSR2", () => {
    void gracefulShutdown("SIGUSR2", () => process.kill(process.pid, "SIGUSR2"));
  });
  process.on("SIGTERM", () => void gracefulShutdown("SIGTERM"));
  process.on("SIGINT", () => void gracefulShutdown("SIGINT"));

  return address;
}

/**
 * Builds and starts the server. Production runs on Bun.serve, development on
 * Fastify with Vite middleware.
 *
 * @param options Server config, the app's route setup, and an optional configure hook.
 * @returns       The bound address.
 */
export async function createServer(options: CreateServerOptions): Promise<string> {
  const { config } = options;

  if (!config.isProduction) {
    return startDevServer(options);
  }

  const { address, stop } = await startBunServer({
    port: config.port,
    host: config.host,
    register: (app) => registerApp(app, options, undefined),
  });
  console.log(`Server started at ${address}`);

  let shuttingDown = false;

  /**
   * Drains in-flight requests before exiting.
   */
  const gracefulShutdown = async () => {
    if (shuttingDown) return;
    shuttingDown = true;

    const force = setTimeout(() => process.exit(1), 10_000);
    force.unref();

    try {
      await stop();
      await shutdownAnalytics();
    } finally {
      process.exit(0);
    }
  };

  process.on("SIGTERM", () => void gracefulShutdown());
  process.on("SIGINT", () => void gracefulShutdown());

  return address;
}

/**
 * Builds the POST /__analytics handler. Attributes the beacon's url to the
 * request and forwards it to PostHog.
 *
 * @param config Analytics app name and cookie names.
 * @returns A POST route handler.
 */
function analyticsBeaconHandler(config: {
  /**
   * App name reported on the event.
   */
  app: string;
  /**
   * Cookie holding the auth JWT.
   */
  authCookie?: string;
  /**
   * Consent cookie that must read accepted.
   */
  consentCookie?: string;
}): (request: SunRequest, reply: SunReply) => Promise<void> {
  /**
   * Forwards the beacon to PostHog and acknowledges it.
   */
  return async (request, reply): Promise<void> => {
    const { url } = (request.body as { url?: string } | undefined) ?? {};
    if (url) {
      trackPageView({
        app: config.app,
        url,
        authCookie: config.authCookie,
        consentCookie: config.consentCookie,
        cookie: request.headers.cookie,
        ip: request.ip,
        userAgent: request.headers["user-agent"],
        referrer: request.headers.referer,
      });
    }
    reply.code(204).send();
  };
}

/**
 * Builds the POST /* mutation handler. Dispatches to mutationRegistry and maps
 * MutationError to its HTTP status.
 *
 * @returns A POST route handler.
 */
export function mutationPostHandler(): (request: SunRequest, reply: SunReply) => Promise<void> {
  /**
   * Dispatches the mutation and maps errors to their status.
   */
  return async (request, reply): Promise<void> => {
    const mutationPath = (request.url || "").split("?")[0].slice(1);

    try {
      const result = await mutationRegistry.executeMutation(
        mutationPath,
        (request.body as Record<string, unknown>) ?? {},
        { cookie: request.headers.cookie },
      );
      reply.send(result);
    } catch (error) {
      if (error instanceof MutationError) {
        reply.status(error.statusCode ?? 400).send({ message: error.message });
        return;
      }

      console.error("Error executing mutation:", error);
      reply.status(500).send({ message: "Internal server error" });
    }
  };
}

export type { ServerConfig, SetupRoutes, ConfigureServer, CreateServerOptions };
