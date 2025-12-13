/**
 * @fileoverview Main entry point for the Express server application.
 * Sets up middleware, Vite integration (for development), and routes, then starts the server.
 */

import express from "express";
import path from "path";
import cookieParser from "cookie-parser";
import {
  port,
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

const app = express();

// Serve static files from the 'public' directory
app.use(express.static(path.resolve("./public")));

// Parse cookies from the request headers
app.use(cookieParser());

// Parse JSON bodies
app.use(express.json());

let vite;

// Conditional setup for Vite development server or production static serving
if (!isProduction) {
  // In development, create and use the Vite dev server middleware
  const { createServer } = await import("vite");
  vite = await createServer({
    server: { middlewareMode: true },
    appType: "custom",
    base,
  });
  app.use(vite.middlewares);
} else {
  // In production, use compression and serve static files from the build output
  const compression = (await import("compression")).default;
  const sirv = (await import("sirv")).default;
  const { createProxyMiddleware } = await import("http-proxy-middleware");

  app.use(
    compression({
      brotli: {
        enabled: true,
      },
      filter: (req, res) => {
        if (
          req.accepts("html") &&
          req.method === "GET" &&
          !/\.[^/]+$/.test(req.path)
        ) {
          return false;
        }

        return compression.filter(req, res);
      },
    })
  );

  app.use(
    sirv(path.resolve("dist/client"), {
      extensions: ["html", "js", "css"],
      dev: false,
    })
  );
  app.use("/messages", express.static(path.resolve("./messages")));

  app.use(
    "/api",
    createProxyMiddleware({
      target: `http://${backendHost}:${backendPort}`,
      changeOrigin: true,
      pathRewrite: { "^/api": "" },
      secure: false,
    })
  );
}

// Set up all defined application routes
setupRoutes(app, vite);

// Generic POST route for mutations
app.post("*", async (req, res) => {
  const path = req.path.slice(1);
  try {
    const result = await executeMutation(path, req.body);

    if (result.__typename === "QuerySuccess") {
      res.json(result);
    } else if (result.__typename === "StandardError") {
      res.status(400).json(result);
    }
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
          `invalidate_cache=${error.cacheInvalidateKey}; Path=/; Max-Age=31536000; SameSite=Lax;`
        );
      }

      res.setHeader("Set-Cookie", cookieHeaders);

      return res.json({
        __typename: "Redirect",
        redirectTo: error.redirectTo,
      });
    }

    console.error("Error executing mutation:", error);
    res
      .status(500)
      .json({ __typename: "StandardError", message: "Internal server error" });
  }
});

app.listen(port, () => {
  console.log(`Server started at http://localhost:${port}`);
});
