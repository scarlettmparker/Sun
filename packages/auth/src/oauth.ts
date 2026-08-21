/**
 * Options for OAuth URL builder.
 */
export type OAuthUrlBuilderOptions = {
  /**
   * OAuth client id.
   */
  clientId: string;
  /**
   * OAuth redirect URI.
   */
  redirectUri: string;
  /**
   * OAuth scopes.
   */
  scopes: string;
  /**
   * Authorize base URL.
   */
  authorizeBase?: string;
};

/**
 * Creates a builder for OAuth authorize URLs.
 *
 * @param options client id, redirect URI, and scopes
 */
export function createOAuthUrlBuilder(
  options: OAuthUrlBuilderOptions,
): { buildAuthUrl: (state: string) => string } {
  const authorizeBase = options.authorizeBase ?? "https://discord.com/api/oauth2/authorize";
  return {
    /**
     * Builds the authorize URL.
     *
     * @param state the OAuth state nonce
     */
    buildAuthUrl(state: string): string {
      const params = new URLSearchParams({
        client_id: options.clientId,
        redirect_uri: options.redirectUri,
        response_type: "code",
        scope: options.scopes,
        state,
      });
      return `${authorizeBase}?${params.toString()}`;
    },
  };
}

/**
 * Creates a fresh OAuth state nonce.
 */
export function newOAuthState(): string {
  return crypto.randomUUID();
}

/**
 * Legacy random state.
 */
export function newState(): string {
  return Math.random().toString(36).slice(2);
}

/**
 * Strips a trailing slash except for root.
 *
 * @param pathname the pathname to normalize
 */
export function normalizePath(pathname: string): string {
  return pathname.length > 1 && pathname.endsWith("/") ? pathname.replace(/\/+$/, "") : pathname;
}
