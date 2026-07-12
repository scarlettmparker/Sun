import fs from "fs";
import path from "path";
import type { ReactElement } from "react";
import { renderToPipeableStream } from "react-dom/server";
import { inlineCss, generateCssTag } from "@sun/utils/css-inlining";
import { getRequestCache, invalidateCache } from "./page-data";
import type { MutationResult } from "./client-mutation";

/** Locale candidates tried, in order, when loading the consolidated messages file. */
const LOCALE_FALLBACK = ["en-GB", "en"];

/**
 * Per-route SEO metadata injected into the document head.
 */
export type RouteMeta = {
  /**
   * Page title; falls back to the app default.
   */
  title?: string;
  /**
   * Meta description.
   */
  description?: string;
  /**
   * Absolute or root-relative Open Graph image URL.
   */
  ogImage?: string;
};

/**
 * Resolved theme bundle for inlining and the client theme switcher.
 */
export type ResolvedTheme = {
  /**
   * Active theme values, inlined as CSS custom properties.
   */
  current: Record<string, string> | null;
  /**
   * Every available theme, exposed via window.__themes__.
   */
  all: { name: string; values: Record<string, string> }[];
};

/**
 * Inputs the app computes per request and passes to the renderer.
 */
export type RenderOptions = {
  /**
   * The React tree to render, already chosen for match/no-match.
   */
  app: ReactElement;
  /**
   * Whether the URL matched a route (sets the status code).
   */
  didMatch: boolean;
  /**
   * Request URL.
   */
  url: string;
  /**
   * Resolved locale code.
   */
  locale: string;
  /**
   * First path segment, used only for diagnostics.
   */
  pageName: string;
  /**
   * Client entry chunk URL.
   */
  clientJs: string;
  /**
   * Client CSS chunk URLs.
   */
  clientCss: string[];
  /**
   * True in production.
   */
  isProduction: boolean;
  /**
   * Toast payload from a prior redirect, if any.
   */
  mutationPayload?: MutationResult | null;
  /**
   * Invalidate-cache cookie value, if present.
   */
  invalidateCacheCookie?: string;
  /**
   * Frontend mode flag, emitted when configured.
   */
  frontendMode?: string;
  /**
   * Matched route's SEO metadata, if any.
   */
  meta?: RouteMeta;
};

/**
 * App-specific renderer configuration supplied once to createRenderer.
 */
export type AppRenderConfig = {
  /**
   * Default document title.
   */
  title: string;
  /**
   * Optional theme resolver; returning null skips theme inlining.
   */
  resolveTheme?: (opts: {
    locale: string;
    pageName: string;
  }) => Promise<ResolvedTheme | null>;
  /**
   * Optional hook initialising the server-side i18n instance with the loaded
   * namespace bundles.
   */
  initI18n?: (
    locale: string,
    translations: Record<string, unknown>,
  ) => Promise<unknown> | unknown;
  /**
   * Emits window.__posthog_key__/__posthog_host__ when true.
   */
  posthog?: boolean;
  /**
   * Emits window.__FRONTEND_MODE__ when true.
   */
  emitFrontendMode?: boolean;
  /**
   * Extra window globals merged into the prelude script.
   */
  windowGlobals?: (opts: RenderOptions) => Record<string, unknown>;
};

/**
 * Output of a render, consumed by the stream piper.
 */
export type RenderResult = {
  /**
   * HTTP status code (200 or 404).
   */
  statusCode: number;
  /**
   * Response headers.
   */
  headers: Record<string, string>;
  /**
   * HTML from DOCTYPE through the opening #app div.
   */
  prelude: string;
  /**
   * Closing HTML, or a builder called once the stream ends.
   */
  postlude: string | (() => string);
  /**
   * The React pipeable stream.
   */
  stream: ReturnType<typeof renderToPipeableStream>;
};

function loadTranslations(locale: string): Record<string, unknown> {
  for (const candidate of [locale, ...LOCALE_FALLBACK]) {
    const filePath = path.resolve(process.cwd(), `messages/${candidate}.json`);
    if (fs.existsSync(filePath)) {
      try {
        return JSON.parse(fs.readFileSync(filePath, "utf-8"));
      } catch {
        return {};
      }
    }
  }
  return {};
}

function escapeAttr(value: string): string {
  return value.replace(/"/g, "&quot;");
}

function metaTags(meta: RouteMeta | undefined, fallbackTitle: string): string {
  const title = meta?.title ?? fallbackTitle;
  const tags = [`<title>${title}</title>`];
  if (meta?.description) {
    const desc = escapeAttr(meta.description);
    tags.push(`<meta name="description" content="${desc}" />`);
    tags.push(`<meta property="og:description" content="${desc}" />`);
  }
  if (meta?.ogImage) {
    tags.push(
      `<meta property="og:image" content="${escapeAttr(meta.ogImage)}" />`,
    );
  }
  if (meta?.title) {
    tags.push(
      `<meta property="og:title" content="${escapeAttr(meta.title)}" />`,
    );
  }
  tags.push(`<meta property="og:type" content="website" />`);
  return tags.join("\n              ");
}

function themeStyle(theme: Record<string, string> | null): string {
  if (!theme) return "";
  const body = Object.entries(theme)
    .map(([key, value]) => `--${key}:${value};`)
    .join("");
  return `<style>:root{${body}}</style>`;
}

function refreshPreamble(isProduction: boolean): string {
  if (isProduction) return "";
  const base = process.env.VITE_SERVER_BASE ?? "";
  return `<script type="module">
              import RefreshRuntime from '${base}/@react-refresh'
              RefreshRuntime.injectIntoGlobalHook(window)
              window.$RefreshReg$ = () => {}
              window.$RefreshSig$ = () => (type) => type
              window.__vite_plugin_react_preamble_installed__ = true
            </script>`;
}

/**
 * Builds a render function from app config.
 */
export function createRenderer(config: AppRenderConfig): {
  /**
   * Renders the app tree to a streamed HTML result.
   */
  render: (options: RenderOptions) => Promise<RenderResult>;
} {
  return {
    render: (options) => renderApp(config, options),
  };
}

async function renderApp(
  config: AppRenderConfig,
  options: RenderOptions,
): Promise<RenderResult> {
  const {
    app,
    didMatch,
    locale,
    clientJs,
    clientCss,
    isProduction,
    invalidateCacheCookie,
    frontendMode,
    meta,
  } = options;

  if (!clientJs) {
    throw new Error("Missing required clientJs path");
  }

  let shouldDeleteCookie = false;
  if (invalidateCacheCookie) {
    shouldDeleteCookie = invalidateCache(invalidateCacheCookie);
  }

  // Capture the request cache now; onAllReady fires outside the ALS context.
  const requestCache = getRequestCache();
  for (const [key, record] of requestCache.entries()) {
    if (record.status === "rejected") {
      requestCache.delete(key);
    }
  }

  const translations = loadTranslations(locale);
  await config.initI18n?.(locale, translations);

  const theme = config.resolveTheme
    ? await config.resolveTheme({ locale, pageName: options.pageName })
    : null;
  const currentTheme = theme?.current ?? null;
  const themes = theme?.all ?? [];

  const extraGlobals = config.windowGlobals?.(options) ?? {};
  const posthogKey = config.posthog ? (process.env.POSTHOG_API_KEY ?? "") : "";
  const posthogHost = config.posthog ? (process.env.POSTHOG_HOST ?? "") : "";

  const cssContent = await inlineCss(isProduction, clientCss);

  return new Promise((resolve) => {
    let resolved = false;
    let postludeData = "";

    // Never pass bootstrapModules: it lands before the postlude fills
    // window.__serverCacheData__, so the client boots too early on first load.
    const stream = renderToPipeableStream(app, {
      onShellReady() {
        const cssTag = generateCssTag(isProduction, cssContent, clientCss);
        const headers: Record<string, string> = { "Content-Type": "text/html" };
        if (shouldDeleteCookie) {
          headers["Set-Cookie"] =
            "invalidate_cache=; Path=/; Max-Age=0; SameSite=Lax;";
        }
        const globals = Object.entries({
          __translations__: translations,
          __locale__: locale,
          __theme__: currentTheme,
          __themes__: themes,
          __serverCacheData__: {},
          ...(config.posthog
            ? { __posthog_key__: posthogKey, __posthog_host__: posthogHost }
            : {}),
          ...(config.emitFrontendMode && frontendMode
            ? { __FRONTEND_MODE__: frontendMode }
            : {}),
          ...extraGlobals,
        })
          .map(
            ([k, v]) =>
              `window.${k} = ${k.startsWith("__locale") ? `'${v}'` : JSON.stringify(v)};`,
          )
          .join("\n              ");

        const prelude = `<!DOCTYPE html>
          <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              ${cssTag}
              ${themeStyle(currentTheme)}
              <link rel="modulepreload" href="${clientJs}" />
              ${metaTags(meta, config.title)}
            </head>
            ${refreshPreamble(isProduction)}
            <script>
              ${globals}
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
        for (const [key, record] of requestCache.entries()) {
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
