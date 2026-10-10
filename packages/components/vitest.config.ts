import { defineConfig } from "vitest/config";
import path from "node:path";

export default defineConfig({
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./testing/vitest/setup.ts"],
    include: ["__tests__/**/*.test.{ts,tsx}"],
    css: false,
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      reporter: ["text", "html"],
    },
  },
  resolve: {
    alias: [
      { find: /^@sun\/components$/, replacement: path.resolve(import.meta.dirname, "./src/index.ts") },
      { find: /^.+\.module\.css$/, replacement: path.resolve(import.meta.dirname, "./testing/vitest/css-module-mock.ts") },
      { find: /^.+\.css$/, replacement: path.resolve(import.meta.dirname, "./testing/vitest/css-module-mock.ts") },
      { find: "~", replacement: path.resolve(import.meta.dirname, "./src") },
    ],
  },
});
