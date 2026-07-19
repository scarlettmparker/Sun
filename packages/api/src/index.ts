import { print, type DocumentNode } from "graphql";
import { getRequestCookie } from "@sun/ssr";

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: string;
  statusCode?: number;
};

let authCookieName: string | undefined;

/**
 * Sets the auth cookie name used to forward the caller's JWT.
 */
export function configureApi(config: { authCookie?: string }): void {
  authCookieName = config.authCookie;
}

/**
 * Reads a named value from a Cookie header.
 */
export function getCookieValue(
  cookieHeader: string | undefined,
  name: string,
): string | undefined {
  if (!cookieHeader) return undefined;
  for (const part of cookieHeader.split(/;\s*/)) {
    const index = part.indexOf("=");
    if (index < 0) continue;
    if (part.slice(0, index).trim() === name) {
      return decodeURIComponent(part.slice(index + 1));
    }
  }
  return undefined;
}

function resolveAuthToken(authToken?: string): string | undefined {
  if (authToken) {
    return authToken;
  }
  const cookie = getRequestCookie();
  return cookie && authCookieName
    ? getCookieValue(cookie, authCookieName)
    : undefined;
}

/**
 * Runs a GraphQL document against the backend, forwarding the caller's JWT.
 */
export async function executeDocument<T, V = Record<string, unknown>>(
  document: DocumentNode,
  variables?: V,
  authToken?: string,
): Promise<ApiResponse<T>> {
  const endpoint =
    process.env.GRAPHQL_ENDPOINT || "http://localhost:8083/graphql";
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  const token = resolveAuthToken(authToken);
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers,
      body: JSON.stringify({ query: print(document), variables }),
    });
    if (!response.ok) {
      return {
        success: false,
        error: `HTTP ${response.status}: ${response.statusText}`,
        statusCode: response.status,
      };
    }
    const result = await response.json();
    if (result.errors) {
      return {
        success: false,
        error: result.errors
          .map((e: { message: string }) => e.message)
          .join(", "),
        statusCode: 400,
      };
    }
    if (!result.data) {
      return { success: false, error: "No data returned", statusCode: 400 };
    }
    return { success: true, data: result.data };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : "Unknown error",
      statusCode: 500,
    };
  }
}
