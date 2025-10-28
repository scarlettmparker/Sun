import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "~": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 3000,
  },
  assetsInclude: ["**/*.json"],
  json: {
    stringify: true,
  },
  build: {
    manifest: true,
    rollupOptions: {
      input: {
        client: "/src/entry-client.tsx",
      },
    },
    outDir: "dist/client",
    cssCodeSplit: true,
  },
  ssr: {
    noExternal: ["react-router-dom"],
  },
});
