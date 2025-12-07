/**
 * @fileoverview Server-Side Rendering (SSR) utilities for rendering the React application.
 * @module utils/ssr
 */

import { Writable } from "stream";
import path from "path";
import fs from "fs/promises";
import { manifestPath } from "../config.js";

let manifest;

/**
 * Loads the Vite client manifest file for production builds.
 * @returns {Promise<object>} A promise that resolves to the parsed manifest object.
 * @throws {Error} If the manifest file cannot be read or parsed.
 */
async function loadManifest() {
  if (!manifest) {
    manifest = JSON.parse(
      await fs.readFile(path.resolve(manifestPath), "utf-8")
    );
  }
  return manifest;
}

/**
 * Renders the React application on the server and streams the HTML response to the client.
 *
 * @param {object} params - Parameters for rendering.
 * @param {import("vite").ViteDevServer} [params.vite] - Vite dev server instance (only in development).
 * @param {boolean} params.isProduction - True if the server is running in production mode.
 * @param {string} params.url - original URL of the request.
 * @param {string} params.locale - Current locale.
 * @param {string} params.pageName - Name of the current page.
 * @param {object|null} params.user - Authenticated user object, or null if not authenticated.
 * @param {object} params.pageData - Pre-fetched data for the current page.
 * @param {import("express").Response} res - Express response object to which the HTML will be streamed.
 * @returns {Promise<void>} A promise that resolves when the rendering and streaming are complete.
 */
export async function renderApp(
  { vite, isProduction, url, locale, pageName, user, pageData },
  res
) {
  try {
    let render;
    let clientJs, clientCss;

    if (!isProduction) {
      render = (await vite.ssrLoadModule("/src/entry-server.tsx")).render;
      clientJs = "/src/entry-client.tsx";
      clientCss = ["/src/styles/globals.css"];
    } else {
      const productionManifest = await loadManifest();
      render = (await import("../dist/server/entry-server.js")).render;
      clientJs = "/" + productionManifest["src/entry-client.tsx"].file;

      // Collect all CSS files from the manifest
      const allCss = new Set();
      for (const [_, chunk] of Object.entries(productionManifest)) {
        if (chunk.css) {
          chunk.css.forEach((css) => allCss.add("/" + css));
        }
      }
      clientCss = Array.from(allCss);
    }

    // Pass user and other data to SSR render function
    const rendered = await render({
      url,
      locale,
      pageName,
      clientJs,
      clientCss,
      user,
      pageData,
      isProduction,
    });

    res.statusCode = rendered.statusCode;
    for (const [key, value] of Object.entries(rendered.headers)) {
      res.setHeader(key, value);
    }

    res.write(rendered.prelude);

    const writable = new Writable({
      write(chunk, _encoding, callback) {
        res.write(chunk);
        callback();
      },
      final(callback) {
        res.write(rendered.postlude);
        res.end();
        callback();
      },
    });

    rendered.stream.pipe(writable);
  } catch (e) {
    if (!isProduction) {
      vite?.ssrFixStacktrace(e);
    }
    console.error("SSR rendering error:", e.stack);
    res.status(500).end(e.stack);
  }
}
