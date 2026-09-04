import fs from "fs";
import path from "path";
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
  async onSuccess() {
    const out = path.resolve("dist/index.css");
    if (!fs.existsSync(out)) return;
    const raw = await fs.promises.readFile(out, "utf8");
    const stripped = raw.replace(/\/\*# sourceMappingURL=.*?\*\//g, "").trim();
    const { code } = transform({
      filename: "index.css",
      code: Buffer.from(stripped),
      minify: true,
    });
    await fs.promises.writeFile(out, code.toString());
    const mapPath = `${out}.map`;
    if (fs.existsSync(mapPath)) await fs.promises.unlink(mapPath);
  },
});
