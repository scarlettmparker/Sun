import { Writable } from "node:stream";
import path from "node:path";
import fs from "node:fs/promises";
import type { ViteDevServer } from "vite";
import type { ServerResponse } from "node:http";
import type { MutationResult } from "./client-mutation";

/** Default production manifest path, relative to the app's working directory. */
const DEFAULT_MANIFEST_PATH = "./dist/client/.vite/manifest.json";

// Process-lifetime cache; the manifest is immutable for a given build.
let manifest: Record<string, unknown> | undefined;

/**
 * Inputs to {@link renderApp}, mirroring the app's routes/index.js request data.
 */
export type RenderAppOptions = {
  /**
   * Vite dev server; undefined in production.
   */
  vite?: ViteDevServer;
  /**
   * True in production.
   */
  isProduction: boolean;
  /**
   * Request URL.
   */
  url: string;
  /**
   * Resolved locale code.
   */
  locale: string;
  /**
   * First path segment of the URL.
   */
  pageName: string;
  /**
   * Frontend mode flag.
   */
  frontendMode?: string;
  /**
   * Toast payload from a prior redirect, if any.
   */
  mutationPayload?: MutationResult | null;
  /**
   * Invalidate-cache cookie value, if present.
   */
  invalidateCacheCookie?: string;
  /**
   * Production manifest path; defaults to the standard Vite output.
   */
  manifestPath?: string;
};

async function loadManifest(
  manifestPath: string,
): Promise<Record<string, unknown>> {
  if (!manifest) {
    const raw = await fs.readFile(path.resolve(manifestPath), "utf-8");
    manifest = JSON.parse(raw);
  }
  return manifest!;
}

type EntryRender = (options: {
  url: string;
  locale: string;
  pageName: string;
  clientJs: string;
  clientCss: string[];
  isProduction: boolean;
  mutationPayload?: MutationResult | null;
  invalidateCacheCookie?: string;
  frontendMode?: string;
}) => Promise<{
  statusCode: number;
  headers: Record<string, string>;
  prelude: string;
  postlude: string | (() => string);
  stream: NodeJS.ReadableStream;
}>;

/**
 * Loads the app's entry-server render fn, resolves clientJs/clientCss, renders,
 * and pipes the HTML to the response.
 */
export async function renderApp(
  opts: RenderAppOptions,
  res: ServerResponse,
): Promise<void> {
  const { vite, isProduction, invalidateCacheCookie } = opts;
  try {
    let render: EntryRender;
    let clientJs: string;
    let clientCss: string[];

    if (!isProduction && vite) {
      const entry = (await vite.ssrLoadModule("/src/entry-server.tsx")) as {
        render: EntryRender;
      };
      render = entry.render;
      clientJs = "/src/entry-client-wrapper.js";
      clientCss = [];
    } else {
      const productionManifest = await loadManifest(
        opts.manifestPath ?? DEFAULT_MANIFEST_PATH,
      );
      const entryPath = path.resolve(
        process.cwd(),
        "dist/server/entry-server.js",
      );
      const entry = (await import(entryPath)) as { render: EntryRender };
      render = entry.render;
      const entryChunk = productionManifest["src/entry-client.tsx"] as {
        file: string;
      };
      clientJs = "/" + entryChunk.file;
      const allCss = new Set<string>();
      for (const chunk of Object.values(productionManifest) as {
        css?: string[];
      }[]) {
        if (chunk.css) {
          chunk.css.forEach((css) => allCss.add("/" + css));
        }
      }
      clientCss = Array.from(allCss);
    }

    const rendered = await render({
      url: opts.url,
      locale: opts.locale,
      pageName: opts.pageName,
      clientJs,
      clientCss,
      isProduction,
      mutationPayload: opts.mutationPayload,
      invalidateCacheCookie,
      frontendMode: opts.frontendMode,
    });

    res.statusCode = rendered.statusCode;
    for (const [key, value] of Object.entries(rendered.headers)) {
      res.setHeader(key, value);
    }
    res.write(rendered.prelude);

    // Pipe React's stream into the response; the postlude (closing tags +
    // cache hydration + client script) is written once the stream ends.
    const writable = new Writable({
      write(chunk, _encoding, callback) {
        res.write(chunk);
        callback();
      },
      final(callback) {
        const postludeContent =
          typeof rendered.postlude === "function"
            ? rendered.postlude()
            : rendered.postlude;
        res.write(postludeContent);
        res.end();
        callback();
      },
    });

    rendered.stream.pipe(writable);
  } catch (e) {
    if (!isProduction && vite) {
      vite.ssrFixStacktrace(e as Error);
    }
    console.error("SSR rendering error:", (e as Error).stack);
    res.statusCode = 500;
    res.end((e as Error).stack);
  }
}
