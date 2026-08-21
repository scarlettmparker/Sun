import { type DocumentNode } from "graphql";
import { configureApi, executeDocument } from "@sun/api";

/**
 * Initialises the shared API client.
 *
 * @param opts auth cookie name and per-app credentials
 */
export function initAuth(opts: {
  /**
   * Auth cookie name.
   */
  authCookieName: string;
  /**
   * Per-app client id.
   */
  clientId?: string;
  /**
   * Per-app client secret.
   */
  clientSecret?: string;
  /**
   * Per-app base URL for emailed links.
   */
  appBaseUrl?: string;
}): void {
  configureApi({
    authCookie: opts.authCookieName,
    clientId: opts.clientId,
    clientSecret: opts.clientSecret,
    appBaseUrl: opts.appBaseUrl,
  });
}

/**
 * Creates a loginViaGaia function.
 *
 * @param loginDocument the Login GraphQL document
 */
export function createLoginViaGaia(loginDocument: DocumentNode) {
  /**
   * Logs in via Gaia.
   *
   * @param username the username
   * @param password the password
   */
  return async function loginViaGaia(username: string, password: string): Promise<string | null> {
    const variables = { input: { username, password } };
    const res = await executeDocument<{
      gaiaMutations: { login: { token: string } | null };
    }>(loginDocument, variables);
    if (!res.success || !res.data) {
      return null;
    }
    return res.data.gaiaMutations.login?.token ?? null;
  };
}

/**
 * Options for creating getCurrentUser.
 */
export type GetCurrentUserOptions<TUser> = {
  /**
   * Document for gaiaQueries.me.
   */
  meDocument: DocumentNode;
  /**
   * Optional profile document (e.g. hadesQueries.readerAccount).
   */
  profileDocument?: DocumentNode;
  /**
   * Maps a me result to a synthetic user when profile is null.
   */
  mapFallback?: (me: { id: string; username: string; createdAt?: unknown; updatedAt?: unknown }) => TUser;
};

/**
 * Minimal me shape returned by gaiaQueries.me.
 */
type MeShape = {
  id: string;
  username: string;
  createdAt?: unknown;
  updatedAt?: unknown;
};

/**
 * Creates a getCurrentUser function.
 *
 * @param opts me document, optional profile document, and fallback mapper
 */
export function createGetCurrentUser<TUser>(opts: GetCurrentUserOptions<TUser>) {
  /**
   * Returns the current user for a token.
   *
   * @param token the JWT from the auth cookie
   */
  return async function getCurrentUser(token: string | undefined): Promise<TUser | null> {
    if (!token) {
      return null;
    }
    const meRes = await executeDocument<{
      gaiaQueries: { me: MeShape | null };
    }>(opts.meDocument, {}, token);
    if (!meRes.success) {
      if (meRes.statusCode && meRes.statusCode >= 500) {
        throw new Error(meRes.error || "Transient auth error");
      }
      return null;
    }
    const me = meRes.data?.gaiaQueries.me;
    if (!me) {
      return null;
    }
    if (!opts.profileDocument) {
      return me as TUser;
    }
    const res = await executeDocument<{ hadesQueries: { readerAccount: TUser | null } }>(
      opts.profileDocument,
      {},
      token,
    );
    if (!res.success) {
      if (res.statusCode && res.statusCode >= 500) {
        throw new Error(res.error || "Transient auth error");
      }
      return null;
    }
    const profile = res.data?.hadesQueries.readerAccount;
    if (profile) {
      return profile;
    }
    if (opts.mapFallback) {
      return opts.mapFallback(me);
    }
    return null;
  };
}

/**
 * Creates a logoutViaBackend function.
 *
 * @param logoutDocument optional Logout GraphQL document
 */
export function createLogoutViaBackend(logoutDocument?: DocumentNode) {
  /**
   * Revokes the JWT server-side.
   *
   * @param token the JWT to revoke
   */
  return async function logoutViaBackend(token: string | undefined): Promise<void> {
    if (!token || !logoutDocument) {
      return;
    }
    try {
      await executeDocument(logoutDocument, {}, token);
    } catch (_) {
      // best-effort
    }
  };
}

/**
 * Result of an OAuth code exchange.
 */
export type OAuthLoginResult =
  | {
      /**
       * Login succeeded.
       */
      status: "ok";
      token: string;
    }
  | {
      /**
       * Account requires reactivation.
       */
      status: "deactivated";
    };

/**
 * Creates an OAuth login function.
 *
 * @param doc the OAuth login document
 * @param extract extracts the login payload from the response data
 */
export function createOAuthLogin<T extends { requiresReactivation?: boolean; token: string }>(
  doc: DocumentNode,
  extract: (data: unknown) => T | null,
) {
  /**
   * Exchanges an OAuth code for a JWT.
   *
   * @param code the OAuth code
   * @param state the state nonce
   */
  return async function oauthLoginViaCode(code: string, state: string): Promise<OAuthLoginResult | null> {
    const variables = { code, state };
    const res = await executeDocument(doc, variables);
    if (!res.success || !res.data) {
      return null;
    }
    const login = extract(res.data);
    if (!login) {
      return null;
    }
    if (login.requiresReactivation) {
      return { status: "deactivated" };
    }
    return { status: "ok", token: login.token };
  };
}

/**
 * Creates a Discord login function.
 *
 * @param doc the DiscordLogin document
 */
export function createDiscordLogin(doc: DocumentNode) {
  return createOAuthLogin(doc, (data: unknown) => {
    const d = data as { hadesMutations?: { discordLogin?: { requiresReactivation?: boolean; token: string } | null } };
    return d.hadesMutations?.discordLogin ?? null;
  });
}
