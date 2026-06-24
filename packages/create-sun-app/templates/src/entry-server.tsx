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
import { suspenseCache, invalidateCache, type MutationResult } from "@sun/ssr";
import fs from "fs";
import path from "path";

type i18n = {
  translations: Record<string, string>;
  locale: string;
  pageName: string;
};

type RenderProps = {
  url: string;
  translations: i18n["translations"];
  locale: string;
  pageName: string;
  clientJs: string;
  clientCss: string[];
  isProduction: boolean;
  mutationPayload: MutationResult;
  invalidateCacheCookie?: string;
};

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
  const posthogKey = process.env.POSTHOG_API_KEY ?? "";
  const posthogHost = process.env.POSTHOG_HOST ?? "";

  if (!clientJs) {
    throw new Error("Missing required clientJs path");
  }

  let shouldDeleteCookie = false;
  if (invalidateCacheCookie) {
    shouldDeleteCookie = invalidateCache(invalidateCacheCookie);
  }

  for (const [key, record] of suspenseCache.entries()) {
    if (record.status === "rejected") {
      suspenseCache.delete(key);
    }
  }

  const i18n = createI18nInstance();
  await i18n.init({
    lng: locale,
    fallbackLng: "en",
    resources: {},
    interpolation: { escapeValue: false },
  });

  let translations: Record<string, unknown> = {};
  try {
    const filePath = path.resolve(
      process.cwd(),
      `messages/${pageName}/${locale}.json`,
    );
    if (fs.existsSync(filePath)) {
      const fileContent = fs.readFileSync(filePath, "utf-8");
      translations = JSON.parse(fileContent);
      i18n.addResourceBundle(locale, pageName, translations, true, true);
    }
  } catch {
    // fallback to empty
  }

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

  const cssContent = await inlineCss(isProduction, clientCss);

  return new Promise((resolve) => {
    let resolved = false;
    let postludeData = "";

    const stream = renderToPipeableStream(didMatch ? App : <NotFound />, {
      bootstrapModules: [clientJs],
      onShellReady() {
        const cssTag = generateCssTag(isProduction, cssContent, clientCss);
        const headers: Record<string, string> = { "Content-Type": "text/html" };
        if (shouldDeleteCookie) {
          headers["Set-Cookie"] =
            "invalidate_cache=; Path=/; Max-Age=0; SameSite=Lax;";
        }
        const prelude = `<!DOCTYPE html>
          <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              ${cssTag}
              <title>My App</title>
            </head>
            <script type="module">
              import RefreshRuntime from '${process.env.VITE_SERVER_BASE}/@react-refresh'
              RefreshRuntime.injectIntoGlobalHook(window)
              window.$RefreshReg$ = () => {}
              window.$RefreshSig$ = () => (type) => type
              window.__vite_plugin_react_preamble_installed__ = true
            </script>
            <script>
              window.__translations__ = ${JSON.stringify(translations)};
              window.__posthog_key__ = '${posthogKey}';
              window.__posthog_host__ = '${posthogHost}';
              window.__locale__ = '${locale}';
              window.__serverCacheData__ = {};
            </script>
            <body>
              <div id="app">`;
        if (!resolved) {
          resolved = true;
          resolve({
            statusCode: didMatch ? 200 : 404,
            headers,
            prelude,
            postlude: () => postludeData,
            stream,
          });
        }
      },
      onAllReady() {
        const serverCacheData: Record<string, unknown> = {};

        for (const [key, record] of suspenseCache.entries()) {
          if (record.status === "resolved") {
            serverCacheData[key] = record.result;
          }
        }

        postludeData = `</div>
          <script>
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
