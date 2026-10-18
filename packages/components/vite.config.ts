import { defineConfig } from "vite";
import preact from "@preact/preset-vite";
import dts from "vite-plugin-dts";

export default defineConfig({
  plugins: [
    preact(),
    dts({
      include: ["src/**/*.ts", "src/**/*.tsx"],
      exclude: ["__tests__/**", "testing/**", "src/**/*.test.*"],
    }),
  ],
  build: {
    lib: {
      entry: "src/index.ts",
      formats: ["es", "cjs"],
      fileName: (format) => `index.${format === "es" ? "mjs" : "js"}`,
    },
    cssCodeSplit: false,
    rollupOptions: {
      external: [
        "preact",
        "preact/compat",
        "preact/hooks",
        "preact/jsx-runtime",
      ],
    },
  },
});
