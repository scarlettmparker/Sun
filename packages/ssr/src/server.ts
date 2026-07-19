import path from "path";
import zlib from "node:zlib";
import { AsyncLocalStorage } from "node:async_hooks";
import type { IncomingMessage, ServerResponse } from "node:http";
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import type { ViteDevServer } from "vite";
import { pageDataRpcHandler } from "./rpc-handler";
import { mutationRegistry } from "./mutations";
import { ServerRedirectError } from "./server-redirect";
import { registerSecurity } from "./security";
import { setRequestCacheProvider, setRequestCookieProvider } from "./page-data";
import type { CacheRecord } from "./page-data";

export { handleQuery } from "./query";
export type { QueryFetchResult } from "./query";
export { loadModule } from "./load-module";
export { renderApp } from "./render-stream";
export type { RenderAppOptions } from "./render-stream";
export { createRenderer } from "./renderer";
export type {
  AppRenderConfig,
  RenderOptions,
  RenderResult,
  ResolvedTheme,
  RouteMeta,
} from "./renderer";
export { autoDiscoverRegistrations } from "./auto-discover";

/**
 * Responses smaller than this are sent uncompressed.
 */
const GZIP_THRESHOLD = 1024;
/**
 * Content types worth gzipping. Excludes already-compressed binary (images, video, fonts).
 */
const COMPRESSIBLE_CONTENT_TYPES = /text\/|\+?json|\+?xml|javascript|csv|svg/i;

/**
 * Per-request page-data cache.
 */
const requestCacheAls = new AsyncLocalStorage<Map<string, CacheRecord>>();
setRequestCacheProvider(() => requestCacheAls.getStore() ?? null);

const requestCookieAls = new AsyncLocalStorage<string | undefined>();
setRequestCookieProvider(() => requestCookieAls.getStore() ?? undefined);

type ServerConfig = {
  port: number;
  host: string;
  base: string;
  isProduction: boolean;
  backendHost: string;
  backendPort: number;
  /**
   * HMAC key for CSRF tokens; when unset CSRF protection is disabled.
   */
  clientSecret?: string;
  /**
   * Hostname suffixes allowed by the origin gate.
   */
  allowedOrigins?: string[];
};

/**
 * Registers the app's SSR catch-all and any app-specific GET routes.
 *
 * @param app  The Fastify instance, already wired with dev/prod middleware.
 * @param vite The Vite dev server in development, or undefined in production.
 * @returns    void, or a Promise that resolves once routes are registered.
 */
type SetupRoutes = (
  app: FastifyInstance,
  vite: ViteDevServer | undefined,
) => void | Promise<void>;

/**
 * Optional hook for app-specific global middleware or custom routes. Called after
 * the dev/prod middleware but before /__page-data, setupRoutes and the POST /*
 * mutation route, so global hooks apply to every route.
 *
 * @param app  The Fastify instance.
 * @param vite The Vite dev server in development, or undefined in production.
 * @returns    void, or a Promise that resolves once configuration is done.
 */
type ConfigureServer = (
  app: FastifyInstance,
  vite: ViteDevServer | undefined,
) => void | Promise<void>;

interface CreateServerOptions {
  /**
   * Env-driven server config from the app's config.js.
   */
  config: ServerConfig;
  /**
   * Registers the SSR catch-all (+ any app-specific GET routes).
   */
  setupRoutes: SetupRoutes;
  /**
   * Optional pre-route middleware/custom-route hook (COOP/COEP, download proxy).
   */
  configure?: ConfigureServer;
}

type MiddieHandler = (req: IncomingMessage, res: ServerResponse) => void;

/**
 * Builds and starts the Fastify server with the standard Sun dev/prod middleware,
 * the /__page-data RPC, the app's routes, and the POST /* mutation route.r.
 *
 * @param options Server config, the app's route setup, and an optional configure hook.
 * @returns       The address string returned by app.listen.
 */
export async function createServer(
  options: CreateServerOptions,
): Promise<string> {
  const { config, setupRoutes, configure } = options;
  const { default: Fastify } = await import("fastify");
  const { default: fastifyStatic } = await import("@fastify/static");

  const app: FastifyInstance = Fastify({ logger: false });
  let vite: ViteDevServer | undefined;

  if (!config.isProduction) {
    await app.register(fastifyStatic, {
      root: path.resolve("./public"),
      prefix: "/",
      decorateReply: false,
    });
    const { default: fastifyMiddie } = await import("@fastify/middie");
    await app.register(fastifyMiddie);
    const { createServer: createViteServer } = await import("vite");
    vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "custom",
      base: config.base,
    });
    app.use(vite.middlewares as unknown as MiddieHandler);
  } else {
    await app.register(fastifyStatic, {
      root: path.resolve("dist/client"),
      prefix: "/",
      decorateReply: false,
    });
    await app.register(fastifyStatic, {
      root: path.resolve("./messages"),
      prefix: "/messages/",
      decorateReply: false,
    });
  }

  // Per-request page-data cache. Must run before any route handler so the
  // AsyncLocalStorage context is active for the whole request (including the
  // SSR render and its onAllReady callback).
  app.addHook("onRequest", async (request) => {
    requestCacheAls.enterWith(new Map());
    requestCookieAls.enterWith(request.headers.cookie);
  });

  // App-layer gzip for buffered (string/Buffer) JSON/text responses.
  app.addHook("onSend", (req, reply, payload, done) => {
    if (payload == null) {
      return done();
    }

    if (typeof payload !== "string" && !Buffer.isBuffer(payload)) {
      return done();
    }

    if (reply.getHeader("content-encoding")) {
      return done();
    }

    const acceptEncoding = req.headers["accept-encoding"] ?? "";
    if (
      typeof acceptEncoding !== "string" ||
      !acceptEncoding.includes("gzip")
    ) {
      return done();
    }
    const contentType = String(reply.getHeader("content-type") ?? "");
    if (contentType && !COMPRESSIBLE_CONTENT_TYPES.test(contentType)) {
      return done();
    }

    const buf = Buffer.isBuffer(payload) ? payload : Buffer.from(payload);

    if (buf.length < GZIP_THRESHOLD) {
      return done();
    }

    reply.header("content-encoding", "gzip");
    reply.header("vary", "accept-encoding");
    reply.removeHeader("content-length");
    done(null, zlib.gzipSync(buf));
  });

  if (configure) {
    await configure(app, vite);
  }

  // Origin gate + signed-token CSRF protection. Runs after body-parser plugins
  // (formbody) so the preHandler can read the _csrf form field on native posts.
  registerSecurity(app, {
    clientSecret: config.clientSecret,
    allowedOrigins: config.allowedOrigins,
    isProduction: config.isProduction,
  });

  app.post("/__page-data", pageDataRpcHandler());

  await setupRoutes(app, vite);

  app.route({
    method: "POST",
    url: "/*",
    handler: mutationPostHandler(),
  });

  const address = await app.listen({ port: config.port, host: config.host });
  console.log(`Server started at ${address}`);

  // Graceful shutdown: drain in-flight requests before exiting so a restart
  // (nodemon SIGUSR2, deploy SIGTERM, Ctrl-C) doesn't truncate responses mid-
  // stream - which a reverse proxy surfaces as an empty 200 and breaks the
  // client's page-data RPC for the rest of the session.
  let shuttingDown = false;
  const gracefulShutdown = async (reason: string, done?: () => void) => {
    if (shuttingDown) return;
    shuttingDown = true;
    console.log(`${reason} received, draining in-flight requests…`);

    // Don't let a stuck request hold the restart hostage.
    const force = setTimeout(() => {
      console.error("Force-exiting after shutdown timeout");
      process.exit(1);
    }, 10_000);
    force.unref();

    try {
      await app.close();
    } catch (err) {
      console.error("Error during graceful shutdown:", err);
    }

    if (done) {
      done();
    } else {
      process.exit(0);
    }
  };

  // nodemon's default restart signal. Drain, then re-raise so nodemon completes
  // the restart (`.once` keeps the re-raised signal from being caught again).
  process.once("SIGUSR2", () => {
    void gracefulShutdown("SIGUSR2", () =>
      process.kill(process.pid, "SIGUSR2"),
    );
  });
  process.on("SIGTERM", () => void gracefulShutdown("SIGTERM"));
  process.on("SIGINT", () => void gracefulShutdown("SIGINT"));

  return address;
}

/**
 * Builds the POST /* mutation handler: dispatches to mutationRegistry, stamps the
 * redirect/payload/invalidate cookies on ServerRedirectError, and maps result
 * __typename values to HTTP status codes.
 *
 * @returns A Fastify POST route handler.
 */
export function mutationPostHandler(): (
  request: FastifyRequest,
  reply: FastifyReply,
) => Promise<void> {
  return async (request, reply): Promise<void> => {
    const mutationPath = (request.url || "").split("?")[0].slice(1);

    try {
      const result = await mutationRegistry.executeMutation(
        mutationPath,
        (request.body as Record<string, unknown>) ?? {},
        { cookie: request.headers.cookie },
      );

      if (result.invalidated && result.invalidated.length) {
        reply.header(
          "Set-Cookie",
          `invalidate_cache=${encodeURIComponent(JSON.stringify(result.invalidated))}; Path=/; Max-Age=31536000; SameSite=Lax;`,
        );
      }

      if (result.__typename === "QuerySuccess") {
        reply.send(result);
        return;
      }
      if (result.__typename === "StandardError") {
        reply.status(400).send(result);
        return;
      }
      reply.send(result);
    } catch (error) {
      if (error instanceof ServerRedirectError) {
        const payloadString = JSON.stringify(error.clientPayload ?? {});
        const encodedPayload = Buffer.from(payloadString).toString("base64");

        const cookieHeaders: string[] = [
          `mutation_payload=${encodedPayload}; Path=/; Max-Age=5; SameSite=Lax;`,
          `redirect_to=${error.redirectTo}; Path=/; Max-Age=5; SameSite=Lax;`,
        ];

        if (error.cacheInvalidateKey) {
          const encodedCacheKey = encodeURIComponent(
            JSON.stringify(
              Array.isArray(error.cacheInvalidateKey)
                ? error.cacheInvalidateKey
                : [error.cacheInvalidateKey],
            ),
          );
          cookieHeaders.push(
            `invalidate_cache=${encodedCacheKey}; Path=/; Max-Age=31536000; SameSite=Lax;`,
          );
        }

        if (error.cookies && error.cookies.length) {
          cookieHeaders.push(...error.cookies);
        }

        reply.header("Set-Cookie", cookieHeaders);
        reply.send({
          __typename: "Redirect",
          redirectTo: error.redirectTo,
        });
        return;
      }

      console.error("Error executing mutation:", error);
      reply.status(500).send({
        __typename: "StandardError",
        message: "Internal server error",
      });
    }
  };
}

export type { ServerConfig, SetupRoutes, ConfigureServer, CreateServerOptions };
