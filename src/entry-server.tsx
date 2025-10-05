import React from "react";
import { createI18nInstance } from "./utils/i18n";
import { renderToPipeableStream } from "react-dom/server";
import { StaticRouter } from "react-router-dom/server";
import { Router, routes } from "./router";
import Layout from "./layout";
import NotFound from "./routes/not-found";
import { matchRoutes } from "react-router-dom";

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
   * Path or URL to client-side CSS bundle.
   */
  clientCss: string;
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
}: RenderProps) {
  if (!clientJs || !clientCss) {
    throw new Error("Missing required clientJs or clientCss path");
  }

  const i18n = createI18nInstance();
  await i18n.init({
    lng: locale,
    fallbackLng: "en",
    resources: {},
    interpolation: { escapeValue: false },
  });
  const translations = i18n.getResourceBundle(locale, pageName) || {};

  const matches = matchRoutes(routes, url);
  const didMatch = Boolean(matches);

  const App = (
    <React.StrictMode>
      <StaticRouter location={url}>
        <Layout>
          <Router />
        </Layout>
      </StaticRouter>
    </React.StrictMode>
  );

  return new Promise((resolve) => {
    const stream = renderToPipeableStream(
      didMatch ? App : <NotFound />, // Render NotFound if no match
      {
        bootstrapModules: [clientJs],
        onShellReady() {
          resolve({
            statusCode: didMatch ? 200 : 404,
            headers: { "Content-Type": "text/html" },
            prelude: `<!DOCTYPE html>
              <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <link rel="stylesheet" href="${clientCss}" />
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
                  // Inject the translations into the client-side window object
                  window.__translations__ = ${JSON.stringify(translations)};
                  window.__locale__ = '${locale}';
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
      }
    );
  });
}
