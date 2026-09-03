import { defineConfig } from "tsup";

export default defineConfig({
  entry: ["src/index.ts"],
  format: ["cjs", "esm"],
  dts: false,
  clean: true,
  minify: false,
  sourcemap: true,
  external: ["graphql", "@sun/ssr"],
});
