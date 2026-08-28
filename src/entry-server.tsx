import React, { Suspense } from "react";
import { StaticRouter } from "react-router-dom/server";
import { matchRoutes } from "react-router-dom";
import { Router, routes, routeMeta } from "./router";
import Layout from "./components/layout";
import NotFound from "./routes/not-found";
import {
  createRenderer,
  autoDiscoverRegistrations,
  type RouteMeta,
} from "@sun/ssr/server";
import { createI18nInstance } from "./utils/i18n";
import { configureApi } from "@sun/api";
import { AUTH_COOKIE } from "./utils/auth";
import { clientId, clientSecret, base } from "../config.js";
import "./utils/configure-framework";

configureApi({
  authCookie: AUTH_COOKIE,
  clientId,
  clientSecret,
  appBaseUrl: base,
});

// Colocated loaders and mutation handlers self-register at boot.
autoDiscoverRegistrations(import.meta.glob("./**/*-data.ts", { eager: true }));
autoDiscoverRegistrations(
  import.meta.glob("./**/*-mutations.ts", { eager: true }),
);
autoDiscoverRegistrations(
  import.meta.glob("./server/**/*-registrations.ts", { eager: true }),
);

function matchMeta(url: string): RouteMeta | undefined {
  const matches = matchRoutes(routes, url);
  if (!matches) return undefined;
  const composed = matches
    .map((m) => m.route.path ?? "")
    .join("/")
    .replace(/\/{2,}/g, "/");
  return routeMeta[composed === "" ? "/" : composed];
}

const renderer = createRenderer({
  title: "Scarlet Sun",
  initI18n(locale, translations) {
    const i18n = createI18nInstance();
    return i18n.init({
      lng: locale,
      fallbackLng: "en",
      resources: { [locale]: translations } as never,
      interpolation: { escapeValue: false },
      react: { useSuspense: true },
    });
  },
  async resolveTheme() {
    return { current: null, all: [] };
  },
});

export async function render(options: {
  url: string;
  locale: string;
  pageName: string;
  clientJs: string;
  clientCss: string[];
  isProduction: boolean;
  mutationPayload?: unknown;
  invalidateCacheCookie?: string;
  frontendMode?: string;
}) {
  const matches = matchRoutes(routes, options.url);
  const didMatch = Boolean(matches);
  const App = (
    <React.StrictMode>
      <StaticRouter location={options.url}>
        <Layout>
          <Suspense fallback={null}>
            <Router />
          </Suspense>
        </Layout>
      </StaticRouter>
    </React.StrictMode>
  );

  return renderer.render({
    app: didMatch ? App : <NotFound />,
    didMatch,
    meta: matchMeta(options.url),
    url: options.url,
    locale: options.locale,
    pageName: options.pageName,
    clientJs: options.clientJs,
    clientCss: options.clientCss,
    isProduction: options.isProduction,
    mutationPayload: options.mutationPayload as never,
    invalidateCacheCookie: options.invalidateCacheCookie,
    frontendMode: options.frontendMode,
  });
}
