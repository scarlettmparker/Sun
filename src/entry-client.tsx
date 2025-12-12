import { BrowserRouter, useLocation, matchRoutes } from "react-router-dom";
import { Router, routes } from "./router";
import { initReactI18next } from "react-i18next";
import ReactDOM from "react-dom/client";
import i18n from "i18next";
import { Suspense, useEffect } from "react";

import Layout from "./components/layout";
import "./styles/globals.css";
import "./styles/markdown.css";
import "./utils/register-loaders";

/**
 * Get page name from path.
 *
 * @param pathname Path of page.
 */
function getPageName(pathname: string) {
  const page = pathname.split("/")[1];
  return page || "home";
}

/**
 * Dynamically load translations for a given page/locale.
 *
 * @param page Page to load translations for.
 * @param locale Locale.
 */
async function loadTranslations(page: string, locale: string) {
  try {
    const res = await fetch(`/messages/${page}/${locale}.json`);
    if (!res.ok) throw new Error("Not found");
    return await res.json();
  } catch {
    // fallback to en
    const res = await fetch(`/messages/${page}/en.json`);
    return await res.json();
  }
}

/**
 * Wrapper to handle translation loading on route change
 */
function AppWithI18n() {
  const location = useLocation();
  useEffect(() => {
    const locale = window.__locale__ || "en";
    const matches = matchRoutes(routes, location.pathname);
    const page =
      matches && matches[0].route.path === "*"
        ? "not-found"
        : getPageName(location.pathname);
    loadTranslations(page, locale).then((translations) => {
      i18n.addResourceBundle(locale, page, translations, true, true);
      i18n.changeLanguage(locale);
    });
  }, [location.pathname]);
  return (
    <Suspense fallback={null}>
      <Router />
    </Suspense>
  );
}

// Initialize i18n on the client with translations injected from the server
i18n
  .use(initReactI18next)
  .init({
    lng: window.__locale__ || "en",
    resources: {
      [window.__locale__ || "en"]: {
        home: window.__translations__ || {},
        login: window.__translations__ || {},
        blog: window.__translations__ || {},
      },
    },
    interpolation: { escapeValue: false },
    react: { useSuspense: true },
  })
  .then(() => {
    ReactDOM.hydrateRoot(
      document.getElementById("app") as HTMLElement,
      <BrowserRouter>
        <Layout>
          <AppWithI18n />
        </Layout>
      </BrowserRouter>
    );
  })
  .catch((error) => {
    console.error("i18n initialization failed", error);
  });
