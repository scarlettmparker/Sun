import { executeDocument } from "@sun/api";
import {
  LoginDocument,
  LogoutDocument,
  MeDocument,
  type MeQuery,
} from "../generated/graphql";
import { getCookieValue as getCookieValueFromApi } from "@sun/api";

export const AUTH_COOKIE = "sun_auth";

export { getCookieValueFromApi as getCookieValue };

/**
 * Builds the Set-Cookie value that stores the JWT.
 */
export const buildAuthCookie = (
  token: string,
  maxAgeSeconds = 60 * 60 * 12,
): string =>
  `${AUTH_COOKIE}=${encodeURIComponent(token)}; Path=/; HttpOnly; SameSite=Lax; Secure; Max-Age=${maxAgeSeconds}`;

/**
 * Builds the Set-Cookie value that clears the JWT.
 */
export const clearAuthCookie = (): string =>
  `${AUTH_COOKIE}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0`;

/**
 * Logs in via gaia and returns the JWT, or null if rejected.
 */
export const loginViaGaia = async (
  username: string,
  password: string,
): Promise<string | null> => {
  const res = await executeDocument<{
    gaiaMutations: { login: { token: string } | null };
  }>(LoginDocument, { input: { username, password } });
  if (!res.success || !res.data) {
    return null;
  }
  return res.data.gaiaMutations.login?.token ?? null;
};

/**
 * Returns the logged-in account for a JWT, or null.
 * Throws on transient backend errors so the caller can keep the cookie.
 *
 * @param token the JWT from the auth cookie
 * @returns the account or null
 */
export async function getCurrentUser(
  token: string | undefined,
): Promise<MeQuery["gaiaQueries"]["me"] | null> {
  if (!token) {
    return null;
  }
  const res = await executeDocument<MeQuery>(MeDocument, {}, token);
  if (!res.success) {
    if (res.statusCode && res.statusCode >= 500) {
      throw new Error(res.error || "Transient auth error");
    }
    return null;
  }
  return res.data?.gaiaQueries.me ?? null;
}

/**
 * Revokes the current JWT server-side (best-effort).
 *
 * @param token the JWT to revoke
 */
export async function logoutViaBackend(
  token: string | undefined,
): Promise<void> {
  if (!token) return;
  try {
    await executeDocument(LogoutDocument, {}, token);
  } catch {
    // best-effort: cookie will be cleared anyway
  }
}
