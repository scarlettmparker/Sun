import fs from "fs";
import { defineConfig } from "tsup";
import { transform } from "lightningcss";

export default defineConfig({
  entry: ["src/index.ts"],
  format: ["cjs", "esm"],
  dts: true,
  clean: true,
  minify: false,
  sourcemap: true,
  injectStyle: false,
  esbuildPlugins: [
    {
      name: "lightningcss-minify",
      setup(build) {
        build.onLoad({ filter: /\.css$/ }, async (args) => {
          const css = await fs.promises.readFile(args.path, "utf8");
          const { code } = transform({
            filename: args.path,
            code: Buffer.from(css),
            minify: true,
          });
          return { contents: code.toString(), loader: "css" };
        });
      },
    },
  ],
});
