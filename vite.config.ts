import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import { visualizer } from "rollup-plugin-visualizer";
import { compression } from "vite-plugin-compression2";
import path from "path";

export default defineConfig(() => {
  const allowedHosts =
    process.env.ALLOWED_HOSTS?.split(",")
      .map((h) => h.trim())
      .filter(Boolean) ?? [];

  return {
    plugins: [
      react({
        babel: {
          plugins: [["babel-plugin-react-compiler", { target: "19" }]],
        },
      }),
      compression({ algorithm: "gzip", exclude: [/\.(br)$/] }),
      compression({ algorithm: "brotliCompress", exclude: [/\.(gz)$/] }),
    ],
    resolve: {
      dedupe: ["react", "react-dom", "react-i18next", "i18next"],
      alias: {
        "~": path.resolve(__dirname, "./src"),
      },
    },
    optimizeDeps: {
      include: [
        "react",
        "react-dom",
        "react-router-dom",
        "react-i18next",
        "i18next",
      ],
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
      minify: "esbuild",
      cssMinify: true,
      esbuild: {
        drop:
          process.env.NODE_ENV === "production" ? ["console", "debugger"] : [],
      },
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
            if (id.includes("lucide-react") || id.includes("@heroicons")) {
              return "vendor-icons";
            }
            if (id.includes("react-i18next") || id.includes("i18next")) {
              return "vendor-i18n";
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
