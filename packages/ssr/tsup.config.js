import { defineConfig } from "tsup";

export default defineConfig({
  entry: ["src/index.ts", "src/react.ts", "src/server.ts"],
  format: ["cjs", "esm"],
  dts: true,
  clean: true,
  minify: false,
  sourcemap: true,
  external: [
    "react",
    "react-dom",
    "fastify",
    "@fastify/static",
    "@fastify/compress",
    "@fastify/middie",
    "@fastify/http-proxy",
    "vite",
  ],
});