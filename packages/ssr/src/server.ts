import path from "path";
import type { IncomingMessage, ServerResponse } from "node:http";
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import type { ViteDevServer } from "vite";
import { pageDataRpcHandler } from "./rpc-handler";
import { mutationRegistry } from "./mutations";
import { ServerRedirectError } from "./server-redirect";

export { handleQuery } from "./query";
export type { QueryFetchResult } from "./query";
export { loadModule } from "./load-module";

type ServerConfig = {
  port: number;
  host: string;
  base: string;
  isProduction: boolean;
  backendHost: string;
  backendPort: number;
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
    const { default: fastifyCompress } = await import("@fastify/compress");
    await app.register(fastifyCompress, {
      encodings: ["gzip", "br"],
      threshold: 1024,
    });
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
    const { default: fastifyHttpProxy } = await import("@fastify/http-proxy");
    await app.register(fastifyHttpProxy, {
      upstream: `http://${config.backendHost}:${config.backendPort}`,
      prefix: "/api",
      rewritePrefix: "/",
    });
  }

  if (configure) {
    await configure(app, vite);
  }

  app.post("/__page-data", pageDataRpcHandler());

  await setupRoutes(app, vite);

  app.route({
    method: "POST",
    url: "/*",
    handler: mutationPostHandler(),
  });

  const address = await app.listen({ port: config.port, host: config.host });
  console.log(`Server started at ${address}`);
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
