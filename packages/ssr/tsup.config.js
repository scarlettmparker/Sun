import { defineConfig } from "tsup";

export default defineConfig({
  entry: ["src/index.ts", "src/react.ts", "src/server.ts", "src/renderer.ts"],
  format: ["cjs", "esm"],
  dts: true,
  clean: true,
  minify: false,
  sourcemap: true,
  external: [
    "preact",
    "preact/compat",
    "preact/hooks",
    "preact-render-to-string",
    "@sun/analytics",
    "@sun/utils",
    "posthog-node",
    "fastify",
    "@fastify/static",
    "@fastify/compress",
    "@fastify/middie",
    "@fastify/http-proxy",
    "vite",
  ],
});
