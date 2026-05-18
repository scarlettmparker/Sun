/**
 * @fileoverview Main entry point for the Fastify server application.
 * Sets up middleware, Vite integration (for development), and routes, then starts the server.
 */

import Fastify from "fastify";
import fastifyStatic from "@fastify/static";
import fastifyCompress from "@fastify/compress";
import path from "path";
import {
  port,
  host,
  base,
  isProduction,
  backendHost,
  backendPort,
} from "./config.js";
import { setupRoutes } from "./routes/index.js";
import { executeMutation } from "./src/utils/mutations.ts";
import { ServerRedirectError } from "./src/utils/server-redirect";
import { Buffer } from "buffer";

import "./src/utils/register-loaders.ts";
import "./src/utils/register-mutations.ts";

const app = Fastify({ logger: false });
await app.register(fastifyStatic, {
  root: path.resolve("./public"),
  prefix: "/",
  decorateReply: false,
});

let vite;

if (!isProduction) {
  const fastifyMiddie = (await import("@fastify/middie")).default;
  await app.register(fastifyMiddie);

  const { createServer } = await import("vite");
  vite = await createServer({
    server: { middlewareMode: true },
    appType: "custom",
    base,
  });
  app.use(vite.middlewares);
} else {
  await app.register(fastifyCompress, {
    brotli: true,
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

  const fastifyHttpProxy = (await import("@fastify/http-proxy")).default;
  await app.register(fastifyHttpProxy, {
    upstream: `http://${backendHost}:${backendPort}`,
    prefix: "/api",
    rewritePrefix: "/",
  });
}

setupRoutes(app, vite);

app.route({
  method: "POST",
  url: "/*",
  handler: async (request, reply) => {
    const mutationPath = (request.url || "").split("?")[0].slice(1);

    try {
      const result = await executeMutation(mutationPath, request.body);

      if (result.__typename === "QuerySuccess") {
        return reply.send(result);
      }

      if (result.__typename === "StandardError") {
        return reply.status(400).send(result);
      }

      return reply.send(result);
    } catch (error) {
      if (error instanceof ServerRedirectError) {
        const payloadString = JSON.stringify(error.clientPayload || {});
        const encodedPayload = Buffer.from(payloadString).toString("base64");

        const cookieHeaders = [
          `mutation_payload=${encodedPayload}; Path=/; Max-Age=5; SameSite=Lax;`,
          `redirect_to=${error.redirectTo}; Path=/; Max-Age=5; SameSite=Lax;`,
        ];

        if (error.cacheInvalidateKey) {
          cookieHeaders.push(
            `invalidate_cache=${error.cacheInvalidateKey}; Path=/; Max-Age=31536000; SameSite=Lax;`,
          );
        }

        reply.header("Set-Cookie", cookieHeaders);
        return reply.send({
          __typename: "Redirect",
          redirectTo: error.redirectTo,
        });
      }

      console.error("Error executing mutation:", error);
      return reply.status(500).send({
        __typename: "StandardError",
        message: "Internal server error",
      });
    }
  },
});

const address = await app.listen({ port, host });
console.log(`Server started at ${address}`);
