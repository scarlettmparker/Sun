import fs from "fs/promises";
import path from "path";

/**
 * Inlines CSS content for production builds by reading global styles and manifest CSS files.
 *
 * @param isProduction - Whether the application is running in production mode.
 * @param clientCss - Array of client CSS file paths from the manifest.
 *
 * @returns Concatenated CSS content as a string, or empty string if not in production.
 */
export async function inlineCss(
  isProduction: boolean,
  clientCss: string[]
): Promise<string> {
  if (!isProduction) {
    return "";
  }

  let cssContent = "";

  try {
    // Read all CSS files in src/styles
    const stylesDir = path.resolve("src/styles");
    const styleFiles = await fs.readdir(stylesDir);
    const cssFiles = styleFiles.filter((file) => file.endsWith(".css"));
    const stylePromises = cssFiles.map(async (file) => {
      const filePath = path.join(stylesDir, file);
      return await fs.readFile(filePath, "utf-8");
    });
    const styleContents = await Promise.all(stylePromises);
    cssContent += styleContents.join("\n") + "\n";

    // Then read manifest CSS
    if (clientCss && clientCss.length > 0) {
      const cssPromises = clientCss.map(async (css) => {
        const cssPath = path.resolve("dist/client", css.replace(/^\//, ""));
        return await fs.readFile(cssPath, "utf-8");
      });
      const cssContents = await Promise.all(cssPromises);
      cssContent += cssContents.join("\n");
    }
  } catch (error) {
    console.warn("Failed to read CSS files for inlining:", error);
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
  clientCss: string[]
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
