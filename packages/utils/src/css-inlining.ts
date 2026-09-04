import fs from "fs/promises";
import { statSync } from "fs";
import path from "path";

/**
 * Inlines CSS content for production builds by reading global styles and manifest CSS files.
 *
 * @param isProduction - Whether the application is running in production mode.
 * @param clientCss - Array of client CSS file paths from the manifest.
 *
 * @returns Concatenated CSS content as a string, or empty string if not in production.
 */

let cachedKey = "";
let cachedCss = "";

/**
 * Builds a cache key from the max mtime of the given CSS file paths, so a rebuild invalidates
 * the memoised concatenation without re-reading file contents.
 *
 * @param paths - Resolved CSS file paths.
 * @returns A string key, or empty string if no files could be statted.
 */
function buildMtimeKey(paths: string[]): string {
  let key = "";
  for (const p of paths) {
    try {
      const mtime = statSync(p).mtimeMs;
      if (mtime > 0) key += `${p}:${mtime}|`;
    } catch {
      // File may not exist yet (e.g. missing manifest entry); its absence is part of the key.
      key += `${p}:missing|`;
    }
  }
  return key;
}

export async function inlineCss(
  isProduction: boolean,
  clientCss: string[],
): Promise<string> {
  if (!isProduction) {
    return "";
  }

  const stylesDir = path.resolve("src/styles");
  let styleFiles: string[] = [];
  try {
    const files = await fs.readdir(stylesDir);
    styleFiles = files
      .filter((file) => file.endsWith(".css"))
      .map((file) => path.join(stylesDir, file));
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== "ENOENT") {
      throw error;
    }
  }

  const manifestFiles = (clientCss ?? []).map((css) =>
    path.resolve("dist/client", css.replace(/^\//, "")),
  );

  const key = buildMtimeKey([...styleFiles, ...manifestFiles]);
  if (key && key === cachedKey) {
    return cachedCss;
  }

  let cssContent = "";

  try {
    if (styleFiles.length > 0) {
      const styleContents = await Promise.all(
        styleFiles.map((filePath) => fs.readFile(filePath, "utf-8")),
      );
      cssContent += styleContents.join("\n") + "\n";
    }

    if (manifestFiles.length > 0) {
      const cssContents = await Promise.all(
        manifestFiles.map((filePath) => fs.readFile(filePath, "utf-8")),
      );
      cssContent += cssContents.join("\n");
    }
  } catch (error) {
    console.warn("Failed to read CSS files for inlining:", error);
    return cssContent;
  }

  if (key) {
    cachedKey = key;
    cachedCss = cssContent;
  }

  return cssContent;
}

/**
 * Generates the CSS tag for the HTML head based on production mode and CSS content.
 *
 * @param isProduction - Whether the application is running in production mode.
 * @param cssContent - Inlined CSS content.
 * @param clientCss - Array of client CSS file paths from the manifest.
 * @returns HTML string for the CSS tag.
 */
export function generateCssTag(
  isProduction: boolean,
  cssContent: string,
  clientCss: string[],
): string {
  if (isProduction && cssContent) {
    return `<style>${cssContent}</style>`;
  }
  if (clientCss && clientCss.length > 0) {
    return clientCss
      .map((css) => `<link rel="stylesheet" href="${css}" />`)
      .join("");
  }
  return "";
}
