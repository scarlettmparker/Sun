import React, { Suspense } from "react";
import { createI18nInstance } from "./utils/i18n";
import { renderToPipeableStream } from "react-dom/server";
import { StaticRouter } from "react-router-dom/server";
import { Router, routes } from "./router";
import Layout from "./components/layout";
import NotFound from "./routes/not-found";
import { matchRoutes } from "react-router-dom";
import { inlineCss, generateCssTag } from "./utils/css-inlining";
import "./utils/register-loaders";

type i18n = {
  /**
   * Record mapping translation keys to their string values.
   */
  translations: Record<string, string>;

  /**
   * Current locale.
   */
  locale: string;

  /**
   * Name of the page used to load the translation bundle.
   */
  pageName: string;
};

type RenderProps = {
  /**
   * URL of requested route.
   */
  url: string;

  /**
   * Translation strings for current locale/page.
   */
  translations: i18n["translations"];

  /**
   * User's locale.
   */
  locale: string;

  /**
   * Page name corresponding to current route.
   */
  pageName: string;

  /**
   * Path or URL to client-side JS bundle.
   */
  clientJs: string;

  /**
   * Paths or URLs to client-side CSS bundles.
   */
  clientCss: string[];

  /**
   * Pre-fetched data for the current page.
   */
  pageData?: Record<string, unknown>;

  /**
   * Whether running in production mode.
   */
  isProduction: boolean;
};

/**
 * Renders the React application to an HTML stream suitable for server-side rendering.
 * Sets up i18n, initializes the React Router, and returns a promise
 * that resolves when the shell is ready to stream HTML to the client.
 *
 * @returns A promise that resolves with SSR output metadata and stream.
 */
export async function render({
  url,
  locale,
  pageName,
  clientJs,
  clientCss,
  pageData,
  isProduction,
}: RenderProps) {
  if (!clientJs) {
    throw new Error("Missing required clientJs path");
  }

  globalThis.__pageData__ = pageData ?? {};

  const i18n = createI18nInstance();
  await i18n.init({
    lng: locale,
    fallbackLng: "en",
    resources: {},
    interpolation: { escapeValue: false },
  });
  const translations = i18n.getResourceBundle(locale, pageName) || {};

  // Find if page exists (otherwise 404)
  const matches = matchRoutes(routes, url);
  const didMatch = Boolean(matches);

  const App = (
    <React.StrictMode>
      <StaticRouter location={url}>
        <Layout>
          <Suspense fallback={null}>
            <Router />
          </Suspense>
        </Layout>
      </StaticRouter>
    </React.StrictMode>
  );

  // In production, inline CSS to avoid extra fetch
  const cssContent = await inlineCss(isProduction, clientCss);

  return new Promise((resolve) => {
    const stream = renderToPipeableStream(didMatch ? App : <NotFound />, {
      bootstrapModules: [clientJs],
      onShellReady() {
        const cssTag = generateCssTag(isProduction, cssContent, clientCss);
        resolve({
          statusCode: didMatch ? 200 : 404,
          headers: { "Content-Type": "text/html" },
          prelude: `<!DOCTYPE html>
              <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  ${cssTag}
                  <title>Scarlett Sun</title>
                </head>
                <script type="module">
                  import RefreshRuntime from 'http://${process.env.SERVER_BASE || "localhost"}:${process.env.SERVER_PORT || "5173"}/@react-refresh'
                  RefreshRuntime.injectIntoGlobalHook(window)
                  window.$RefreshReg$ = () => {}
                  window.$RefreshSig$ = () => (type) => type
                  window.__vite_plugin_react_preamble_installed__ = true
                </script>
                <script>
                  // Inject the translations and page data into the client-side window object
                  window.__translations__ = ${JSON.stringify(translations)};
                  window.__locale__ = '${locale}';
                  globalThis.__pageData__ = ${JSON.stringify(pageData || {})};
                </script>
                <body>
                  <div id="app">`,
          postlude: `</div>
                  <script type="module" src="${clientJs}"></script>
                </body>
              </html>`,
          stream,
        });
      },
    });
  });
}
