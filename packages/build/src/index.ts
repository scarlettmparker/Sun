/**
 * @fileoverview Shared Vite build plugins for Sun ecosystem apps.
 */

import { readdir, readFile, writeFile } from "node:fs/promises";
import { extname, join } from "node:path";
import { constants, gzipSync } from "node:zlib";
import type { Plugin } from "vite";

/** Extensions that get a `.gz` twin. Compressed formats are left out. */
const DEFAULT_EXTENSIONS = [
  ".js",
  ".mjs",
  ".css",
  ".html",
  ".svg",
  ".json",
  ".txt",
  ".xml",
];

/**
 * Options for {@link compression}.
 */
export type CompressionOptions = {
  /**
   * Extra extensions to compress.
   */
  extensions?: string[];
  /**
   * Skip a twin unless it saves at least this many bytes.
   */
  threshold?: number;
};

/**
 * Yields every file under a directory, skipping Vite's manifest folder.
 *
 * @param dir - Directory to walk.
 * @returns Each file path.
 */
async function* walk(dir: string): AsyncGenerator<string> {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    if (entry.name === ".vite") continue;
    const full = join(dir, entry.name);
    if (entry.isDirectory()) {
      yield* walk(full);
    } else if (entry.isFile()) {
      yield full;
    }
  }
}

/**
 * Writes a `.gz` twin beside each compressible file in the client build, for
 * nginx to serve with `gzip_static on`.
 *
 * @param options - Extensions and size threshold.
 * @returns The Vite plugin.
 */
export function compression(options: CompressionOptions = {}): Plugin {
  const extensions = new Set(
    DEFAULT_EXTENSIONS.concat(options.extensions ?? []),
  );
  const threshold = options.threshold ?? 0;
  let outDir = "dist";
  let ssr = false;

  return {
    name: "@sun/build:compression",
    apply: "build",
    configResolved(config) {
      outDir = config.build.outDir;
      ssr = Boolean(config.build.ssr);
    },
    async closeBundle() {
      if (ssr) return;
      for await (const file of walk(outDir)) {
        if (!extensions.has(extname(file))) continue;
        const source = await readFile(file);
        const compressed = gzipSync(source, {
          level: constants.Z_BEST_COMPRESSION,
        });
        if (source.length - compressed.length < threshold) continue;
        await writeFile(`${file}.gz`, compressed);
      }
    },
  };
}

/**
 * Fails the build when a forbidden package is pulled into a bundle.
 *
 * @param names Package names to reject.
 * @returns The Vite plugin.
 */
export function forbidModules(names: string[]): Plugin {
  return {
    name: "@sun/build:forbid-modules",
    apply: "build",
    enforce: "pre",
    transform(_code, id) {
      for (const name of names) {
        if (id.includes(`/node_modules/${name}/`)) {
          this.error(
            `forbidden module "${name}" was bundled from ${id}`,
          );
        }
      }
      return null;
    },
  };
}
