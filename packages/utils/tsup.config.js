import { defineConfig } from "tsup";

export default defineConfig({
  entry: [
    "src/index.ts",
    "src/cn.ts",
    "src/background-colour.ts",
    "src/css-inlining.ts",
    "src/date.ts",
    "src/posthog.tsx",
    "src/nlp.ts",
    "src/cidr.ts",
    "src/avatar-colour.ts",
    "src/property-set.ts",
  ],
  format: ["cjs", "esm"],
  dts: false,
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
