import { defineConfig } from "vitest/config";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  test: {
    environment: "node",
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
      testing: path.resolve(rootDir, "testing"),
    },
  },
});
