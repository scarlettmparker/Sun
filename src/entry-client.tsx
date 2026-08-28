import { BrowserRouter } from "react-router-dom";
import { Router } from "./router";
import { initReactI18next } from "react-i18next";
import ReactDOM from "react-dom/client";
import i18n from "i18next";
import { Suspense } from "react";
import ErrorBoundary from "./components/error-boundary";
import Layout from "./components/layout";
import { initClientBootstrap } from "@sun/ssr";
import { loadPersistedTheme, applyTheme } from "@sun/themes";
import { PostHogProvider } from "./utils/hooks/posthog";
import "./utils/configure-framework";
import "@sun/components/style.css";
import "@sun/themes/style.css";

// Apply the server-rendered theme before mount so there is no flash; a
// persisted user choice takes precedence.
if (window.localStorage.getItem("sun:theme")) {
  loadPersistedTheme();
} else if ((window as unknown as { __theme__?: unknown }).__theme__) {
  applyTheme((window as unknown as { __theme__: unknown }).__theme__ as never);
}

i18n.use(initReactI18next);

initClientBootstrap({ i18n }).then(() => {
  ReactDOM.hydrateRoot(
    document.getElementById("app") as HTMLElement,
    <PostHogProvider client>
      <BrowserRouter>
        <Layout>
          <ErrorBoundary>
            <Suspense fallback={null}>
              <Router />
            </Suspense>
          </ErrorBoundary>
        </Layout>
      </BrowserRouter>
    </PostHogProvider>,
  );
});
