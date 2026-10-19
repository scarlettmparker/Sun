import { defineConfig } from "vitest/config";
import path from "node:path";

export default defineConfig({
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./testing/vitest/setup.ts"],
    include: ["__tests__/**/*.test.{ts,tsx}"],
    css: false,
    server: {
      deps: {
        inline: ["lucide-react", "@heroicons/react", "react-router-dom", "react-i18next"],
      },
    },
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      reporter: ["text", "html"],
    },
  },
  resolve: {
    alias: [
      {
        find: /^@sun\/components$/,
        replacement: path.resolve(import.meta.dirname, "./src/index.ts"),
      },
      { find: "react-dom/test-utils", replacement: "preact/test-utils" },
      { find: "react-dom", replacement: "preact/compat" },
      { find: "react/jsx-runtime", replacement: "preact/jsx-runtime" },
      { find: "react", replacement: "preact/compat" },
      {
        find: /^.+\.module\.css$/,
        replacement: path.resolve(import.meta.dirname, "./testing/vitest/css-module-mock.ts"),
      },
      {
        find: /^.+\.css$/,
        replacement: path.resolve(import.meta.dirname, "./testing/vitest/css-module-mock.ts"),
      },
      { find: "~", replacement: path.resolve(import.meta.dirname, "./src") },
    ],
  },
});
