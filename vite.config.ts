import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { visualizer } from "rollup-plugin-visualizer";
import path from "path";

export default defineConfig(() => {
  const allowedHosts =
    process.env.ALLOWED_HOSTS?.split(",")
      .map((h) => h.trim())
      .filter(Boolean) ?? [];
  const mainHost = allowedHosts[0];

  return {
    plugins: [react()],
    resolve: {
      alias: {
        "~": path.resolve(__dirname, "./src"),
      },
    },
    server: {
      port: 3000,
      allowedHosts,
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
        plugins: [
          visualizer({
            filename: "stats.html",
            open: false,
            gzipSize: true,
            brotliSize: true,
          }),
        ],
      },
      outDir: "dist/client",
      cssCodeSplit: true,
    },
    ssr: {
      noExternal: ["react-router-dom", "posthog-js", "@posthog/react"],
    },
  };
});
