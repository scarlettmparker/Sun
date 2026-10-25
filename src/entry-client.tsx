import { BrowserRouter } from "react-router-dom";
import { Router } from "./router";
import ReactDOM from "react-dom/client";
import i18n from "i18next";
import { Suspense } from "react";
import ErrorBoundary from "./components/error-boundary";
import Layout from "./components/layout";
import { setI18n } from "@sun/i18n";
import { initClientBootstrap } from "@sun/ssr";
import { ensureDefaultTrustedPolicy } from "@sun/security/trusted-types";
import { loadPersistedTheme, applyTheme } from "@sun/themes";
import "./utils/configure-framework";
import "@sun/components/style.css";
import "@sun/themes/style.css";
import styles from "./suspense-fallback.module.css";

ensureDefaultTrustedPolicy();

// Apply the server-rendered theme before mount
if (window.localStorage.getItem("sun:theme")) {
  loadPersistedTheme();
} else if (window.__theme__) {
  applyTheme(window.__theme__ as never);
}

setI18n(i18n);

initClientBootstrap({ i18n }).then(() => {
  ReactDOM.hydrateRoot(
    document.getElementById("app") as HTMLElement,
    <BrowserRouter>
      <Layout>
        <ErrorBoundary>
          <Suspense fallback={<div className={styles.fallback} aria-hidden />}>
            <Router />
          </Suspense>
        </ErrorBoundary>
      </Layout>
    </BrowserRouter>,
  );
  if (typeof performance !== "undefined" && performance.mark) {
    performance.mark("router:ready");
  }
});
