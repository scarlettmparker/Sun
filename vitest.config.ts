import { defineConfig } from "vitest/config";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  test: {
    environment: "node",
    environmentOptions: { jsdom: { pretendToBeVisual: true } },
    globals: true,
    include: ["__tests__/**/*.test.ts"],
    css: false,
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/generated/**"],
      reporter: ["text", "html"],
    },
  },
  resolve: {
    alias: {
      "~": path.resolve(rootDir, "src"),
      "@ssr": path.resolve(rootDir, "packages/ssr/src"),
      testing: path.resolve(rootDir, "testing"),
      "react-dom/test-utils": "preact/test-utils",
      "react-dom": "preact/compat",
      "react/jsx-runtime": "preact/jsx-runtime",
      react: "preact/compat",
    },
  },
});
