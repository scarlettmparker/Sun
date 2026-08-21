/**
 * Client token store backed by localStorage.
 */
export type ClientTokenStore = {
  /**
   * Storage key.
   */
  TOKEN_KEY: string;
  /**
   * Reads the stored JWT, or null when not in a browser.
   */
  getToken: () => string | null;
  /**
   * Stores the JWT.
   */
  setToken: (token: string) => void;
  /**
   * Clears the stored JWT.
   */
  removeToken: () => void;
};

/**
 * Creates a client-side token store.
 *
 * @param storageKey localStorage key for the token
 */
export function createClientTokenStore(storageKey: string): ClientTokenStore {
  return {
    TOKEN_KEY: storageKey,
    getToken(): string | null {
      if (typeof localStorage === "undefined") {
        return null;
      }
      return localStorage.getItem(storageKey);
    },
    setToken(token: string): void {
      if (typeof localStorage !== "undefined") {
        localStorage.setItem(storageKey, token);
      }
    },
    removeToken(): void {
      if (typeof localStorage !== "undefined") {
        localStorage.removeItem(storageKey);
      }
    },
  };
}
