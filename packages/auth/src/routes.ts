import { Buffer } from "buffer";
import type { FastifyInstance } from "fastify";
import type { ViteDevServer } from "vite";
import { normalizePath } from "./oauth.js";
import type { OAuthLoginResult } from "./auth-helpers.js";

/**
 * OAuth provider config.
 */
export type OAuthProvider = {
  /**
   * Provider id (e.g. discord).
   */
  id: string;
  /**
   * Authorize base URL.
   */
  authorizeBase: string;
  /**
   * Client id.
   */
  clientId: string;
  /**
   * Redirect URI.
   */
  redirectUri: string;
  /**
   * Scopes.
   */
  scopes: string;
  /**
   * State cookie name.
   */
  stateCookie: string;
  /**
   * Callback path.
   */
  callbackPath: string;
  /**
   * Start path.
   */
  startPath: string;
};

/**
 * Options for createAuthRoutes.
 */
export type AuthRoutesOptions = {
  /**
   * Cookie helpers.
   */
  helpers: {
    AUTH_COOKIE: string;
    OAUTH_STATE_COOKIE: string;
    buildAuthCookie: (token: string) => string;
    clearAuthCookie: () => string;
    buildStateCookie: (state: string) => string;
    newOAuthState: () => string;
    getCookieValue: (header: string | undefined, name: string) => string | undefined;
  };
  /**
   * Handlers.
   */
  handlers: {
    loginViaGaia: (username: string, password: string) => Promise<string | null>;
    logoutViaBackend?: (token?: string) => Promise<void>;
    getCurrentUser: (token?: string) => Promise<unknown | null>;
    oauthLogins?: Record<string, (code: string, state: string) => Promise<OAuthLoginResult | null>>;
  };
  /**
   * Config.
   */
  config: {
    base: string;
    isProduction: boolean;
    manifestPath: string;
    PUBLIC_PAGES: Set<string>;
  };
  /**
   * OAuth providers (runtime).
   */
  oauthProviders?: OAuthProvider[];
  /**
   * Redirects.
   */
  redirects?: {
    failure?: string;
    success?: string;
    oauthError?: string;
    deactivated?: string;
  };
  /**
   * Seeds page data before render.
   */
  seedPageData?: (user: unknown | null) => Promise<void>;
  /**
   * Render function from @sun/ssr.
   */
  renderApp: (opts: unknown, res: unknown) => Promise<void>;
};

/**
 * Creates a Fastify setupRoutes function with auth gates.
 *
 * @param opts helpers, handlers, config, and oauth providers
 */
export function createAuthRoutes(opts: AuthRoutesOptions) {
  const helpers = opts.helpers;
  const handlers = opts.handlers;
  const config = opts.config;
  const redirects = opts.redirects ?? {};
  const oauthProviders = opts.oauthProviders ?? [];

  /**
   * Registers auth routes on a Fastify instance.
   *
   * @param app the Fastify instance
   * @param vite the Vite dev server
   */
  return async function setupRoutes(app: FastifyInstance, vite: ViteDevServer | undefined): Promise<void> {
    app.post("/__login", async (request, reply) => {
      const body = (request.body as Record<string, string> | undefined) ?? {};
      const username = body.username ?? "";
      const password = body.password ?? "";
      const token = await handlers.loginViaGaia(username, password);
      if (!token) {
        return reply.redirect(redirects.failure ?? "/login?error=1");
      }
      reply.header("Set-Cookie", helpers.buildAuthCookie(token));
      return reply.redirect(redirects.success ?? "/");
    });

    app.post("/__logout", async (request, reply) => {
      const token = helpers.getCookieValue(request.headers.cookie, helpers.AUTH_COOKIE);
      if (handlers.logoutViaBackend) {
        await handlers.logoutViaBackend(token);
      }
      reply.header("Set-Cookie", helpers.clearAuthCookie());
      return reply.redirect("/login");
    });

    for (const provider of oauthProviders) {
      app.get(provider.startPath, async (_request, reply) => {
        const state = helpers.newOAuthState();
        reply.header("Set-Cookie", helpers.buildStateCookie(state));
        const params = new URLSearchParams({
          client_id: provider.clientId,
          redirect_uri: provider.redirectUri,
          response_type: "code",
          scope: provider.scopes,
          state,
        });
        return reply.redirect(`${provider.authorizeBase}?${params.toString()}`);
      });

      app.get(provider.callbackPath, async (request, reply) => {
        const query = (request.query as Record<string, string> | undefined) ?? {};
        const code = query.code;
        const state = query.state;
        const expected = helpers.getCookieValue(request.headers.cookie, helpers.OAUTH_STATE_COOKIE);
        reply.header("Set-Cookie", `${helpers.OAUTH_STATE_COOKIE}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0`);
        if (!code || !state || !expected || state !== expected) {
          return reply.redirect(redirects.oauthError ?? "/login?error=discord");
        }
        const loginFn = handlers.oauthLogins?.[provider.id];
        if (!loginFn) {
          return reply.redirect(redirects.oauthError ?? "/login?error=discord");
        }
        const result = await loginFn(code, state);
        if (!result) {
          return reply.redirect(redirects.oauthError ?? "/login?error=discord");
        }
        if (result.status === "deactivated") {
          reply.header("Set-Cookie", helpers.clearAuthCookie());
          return reply.redirect(redirects.deactivated ?? "/reactivate");
        }
        reply.header("Set-Cookie", helpers.buildAuthCookie(result.token));
        return reply.redirect(redirects.success ?? "/");
      });
    }

    const { renderApp } = await import("@sun/ssr/server");
    const { base, isProduction, manifestPath } = config;

    app.setNotFoundHandler(async (request, reply) => {
      if (request.method !== "GET") {
        return reply.callNotFound();
      }
      const requestUrl = new URL(request.raw.url ?? "/", "http://localhost");
      const pathname = normalizePath(requestUrl.pathname);

      if (/\.[^/]+$/.test(pathname)) {
        return reply.callNotFound();
      }

      const token = helpers.getCookieValue(request.headers.cookie, helpers.AUTH_COOKIE);
      let user: unknown | null = null;
      let isDeadSession = false;
      let isTransient = false;
      try {
        user = await handlers.getCurrentUser(token);
        isDeadSession = !user && !!token;
      } catch (_) {
        isTransient = !!token;
        isDeadSession = false;
      }
      const isPublic = config.PUBLIC_PAGES.has(pathname);

      if (isDeadSession) {
        reply.header("Set-Cookie", helpers.clearAuthCookie());
      }

      if (!user && !isPublic && !isTransient) {
        return reply.redirect("/login");
      }
      if (user && isPublic) {
        return reply.redirect("/");
      }

      if (opts.seedPageData) {
        await opts.seedPageData(user);
      }

      const mutationPayloadCookie = helpers.getCookieValue(request.headers.cookie, "mutation_payload");
      const invalidateCacheCookie = helpers.getCookieValue(request.headers.cookie, "invalidate_cache");
      let mutationPayload: unknown = null;
      if (mutationPayloadCookie) {
        try {
          mutationPayload = JSON.parse(Buffer.from(mutationPayloadCookie, "base64").toString("utf-8"));
        } catch (_) {
          // ignore
        }
      }

      let url = pathname.replace(base, "");
      if (!url.startsWith("/")) {
        url = `/${url}`;
      }
      if (requestUrl.search) {
        url += requestUrl.search;
      }

      const langHeader = (request.headers["accept-language"] as string | undefined) ?? "en";
      const locale = langHeader.split(",")[0] ?? "en";
      const pathOnly = url.split("?")[0];
      const pageName = pathOnly === "/" ? "library" : pathOnly.split("/")[1] ?? "library";
      const frontendMode = "reader";

      try {
        await renderApp(
          {
            vite,
            isProduction,
            url,
            locale,
            pageName,
            frontendMode,
            mutationPayload,
            invalidateCacheCookie,
            manifestPath,
          } as never,
          reply.raw,
        );
      } catch (e) {
        reply.status(500).send(`Internal Server Error: ${(e as Error).message}`);
      }
    });
  };
}
