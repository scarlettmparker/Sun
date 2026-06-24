/**
 * @fileoverview Main entry point for the Fastify server application.
 * Sets up middleware, Vite integration (for development), and routes, then starts the server.
 */

import { createServer } from "@sun/ssr/server";
import {
  port,
  host,
  base,
  isProduction,
  backendHost,
  backendPort,
} from "./config.js";
import { setupRoutes } from "./routes/index.js";

import "./src/utils/register-loaders.ts";
import "./src/utils/register-mutations.ts";

await createServer({
  config: { port, host, base, isProduction, backendHost, backendPort },
  setupRoutes,
});
