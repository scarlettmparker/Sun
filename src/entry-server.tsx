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
import { suspenseCache } from "./utils/page-data";
import { MutationResult } from "./server/actions/utils";

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
   * Whether running in production mode.
   */
  isProduction: boolean;

  /**
   * Payload for displaying toasts, etc. on client after redirect
   */
  mutationPayload: MutationResult;

  /**
   * Cookie to invalidate the entry-server suspense cache.
   */
  invalidateCacheCookie?: string;
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
  isProduction,
  mutationPayload: _mutationPayload,
  invalidateCacheCookie,
}: RenderProps) {
  if (!clientJs) {
    throw new Error("Missing required clientJs path");
  }

  if (invalidateCacheCookie) {
    suspenseCache.delete(invalidateCacheCookie);
  }

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
    let resolved = false;
    let postludeData = "";

    const stream = renderToPipeableStream(didMatch ? App : <NotFound />, {
      bootstrapModules: [clientJs],
      onShellReady() {
        const cssTag = generateCssTag(isProduction, cssContent, clientCss);
        const prelude = `<!DOCTYPE html>
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
              // Inject the translations and locale into the client-side window object
              window.__translations__ = ${JSON.stringify(translations)};
              window.__locale__ = '${locale}';
              // Initialize server cache data (will be updated in postlude)
              window.__serverCacheData__ = {};
            </script>
            <body>
              <div id="app">`;
        if (!resolved) {
          resolved = true;
          resolve({
            statusCode: didMatch ? 200 : 404,
            headers: { "Content-Type": "text/html" },
            prelude,
            postlude: () => postludeData,
            stream,
          });
        }
      },
      onAllReady() {
        // Collect all resolved data after ALL rendering/data loading completes
        const serverCacheData: Record<string, unknown> = {};

        for (const [key, record] of suspenseCache.entries()) {
          if (record.status === "resolved") {
            serverCacheData[key] = record.result;
          }
        }

        postludeData = `</div>
          <script>
          // Inject server-side cache data so client doesn't re-fetch
          if (window.__serverCacheData__ !== undefined) {
            Object.assign(window.__serverCacheData__, ${JSON.stringify(serverCacheData)});
            if (window.hydratePageDataFromPostlude) {
              window.hydratePageDataFromPostlude(window.__serverCacheData__);
            }
          }
          </script>
          <script type="module" src="${clientJs}"></script>
        </body>
      </html>`;
      },
    });
  });
}
