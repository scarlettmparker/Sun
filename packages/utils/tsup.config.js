import { defineConfig } from "tsup";

export default defineConfig({
  entry: [
    "src/index.ts",
    "src/cn.ts",
    "src/background-colour.ts",
    "src/css-inlining.ts",
    "src/date.ts",
    "src/posthog.tsx",
  ],
  format: ["cjs", "esm"],
  dts: true,
  clean: true,
  minify: false,
  sourcemap: true,
  external: [
    "react",
    "react-dom",
    "react/jsx-runtime",
    "@posthog/react",
    "posthog-js",
    "posthog-node",
  ],
});
