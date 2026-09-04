import { defineConfig } from "tsup";

export default defineConfig({
  entry: ["src/index.ts", "src/trusted-types.ts"],
  format: ["cjs", "esm"],
  dts: true,
  clean: true,
  minify: false,
  sourcemap: true,
});
