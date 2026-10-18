import { defineConfig } from "tsup";

export default defineConfig({
  entry: ["src/client.ts", "src/server.ts"],
  format: ["cjs", "esm"],
  dts: true,
  clean: true,
  minify: false,
  sourcemap: true,
  external: ["posthog-node"],
});
