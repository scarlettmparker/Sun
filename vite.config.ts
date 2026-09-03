import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import { visualizer } from "rollup-plugin-visualizer";
import path from "path";

export default defineConfig(() => {
  const allowedHosts =
    process.env.ALLOWED_HOSTS?.split(",")
      .map((h) => h.trim())
      .filter(Boolean) ?? [];

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
          ...(process.env.ANALYZE
            ? [
                visualizer({
                  filename: "stats.html",
                  open: false,
                  gzipSize: true,
                  brotliSize: true,
                }),
              ]
            : []),
        ],
        output: {
          manualChunks(id) {
            if (!id.includes("node_modules")) {
              return;
            }
            if (id.includes("@sun/components")) {
              return "vendor-components";
            }
            if (id.includes("posthog")) {
              return "vendor-posthog";
            }
            if (
              id.includes("/react-dom/") ||
              id.includes("\\react-dom\\") ||
              id.includes("/react-router-dom/") ||
              id.includes("\\react-router-dom\\") ||
              /[/\\]react[/\\]/.test(id)
            ) {
              return "vendor-react";
            }
          },
        },
      },
      outDir: "dist/client",
      cssCodeSplit: true,
    },
    ssr: {
      noExternal: ["react-router-dom"],
      external: ["@sun/ssr"],
    },
  };
});
