/**
 * Options for creating cookie helpers.
 */
export type CookieHelperOptions = {
  /**
   * Name of the httpOnly auth cookie.
   */
  authCookieName: string;
  /**
   * Name of the OAuth state cookie.
   */
  oauthStateCookie?: string;
  /**
   * Default Max-Age for the auth cookie in seconds.
   */
  defaultMaxAge?: number;
};

/**
 * Helpers for building auth cookies.
 */
export type CookieHelpers = {
  /**
   * Auth cookie name.
   */
  AUTH_COOKIE: string;
  /**
   * OAuth state cookie name.
   */
  OAUTH_STATE_COOKIE: string;
  /**
   * Builds the Set-Cookie for the auth JWT.
   */
  buildAuthCookie: (token: string, maxAgeSeconds?: number) => string;
  /**
   * Builds the Set-Cookie that clears the auth cookie.
   */
  clearAuthCookie: () => string;
  /**
   * Builds the Set-Cookie for the OAuth state nonce.
   */
  buildStateCookie: (state: string) => string;
};

/**
 * Creates cookie helpers for an app.
 *
 * @param options auth cookie name, optional state cookie, and default max age
 */
export function createCookieHelpers(options: CookieHelperOptions): CookieHelpers {
  const authCookieName = options.authCookieName;
  const oauthStateCookie = options.oauthStateCookie ?? "oauth_state";
  const defaultMaxAge = options.defaultMaxAge ?? 60 * 60 * 24;

  return {
    AUTH_COOKIE: authCookieName,
    OAUTH_STATE_COOKIE: oauthStateCookie,
    /**
     * Builds the auth cookie.
     *
     * @param token the JWT to store
     * @param maxAgeSeconds optional TTL, defaults to defaultMaxAge
     */
    buildAuthCookie(token: string, maxAgeSeconds = defaultMaxAge): string {
      return `${authCookieName}=${encodeURIComponent(token)}; Path=/; HttpOnly; SameSite=Lax; Secure; Max-Age=${maxAgeSeconds}`;
    },
    /**
     * Clears the auth cookie.
     */
    clearAuthCookie(): string {
      return `${authCookieName}=; Path=/; HttpOnly; SameSite=Lax; Secure; Max-Age=0`;
    },
    /**
     * Builds the state cookie.
     *
     * @param state the OAuth state nonce
     */
    buildStateCookie(state: string): string {
      return `${oauthStateCookie}=${encodeURIComponent(state)}; Path=/; HttpOnly; SameSite=Lax; Max-Age=600`;
    },
  };
}
