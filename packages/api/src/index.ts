import { print, type DocumentNode } from "graphql";
import { getRequestCookie, getRequestIp } from "@sun/ssr";

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: string;
  statusCode?: number;
};

let authCookieName: string | undefined;
let clientSecret: string | undefined;
let clientId: string | undefined;
let apiKey: string | undefined;
let appBaseUrl: string | undefined;

/**
 * Sets the auth cookie name and per-app backend credentials. Server-only: call
 * from the app's entry-server bootstrap, never the shared client bootstrap.
 */
export function configureApi(config: {
  authCookie?: string;
  clientSecret?: string;
  clientId?: string;
  apiKey?: string;
  appBaseUrl?: string;
}): void {
  authCookieName = config.authCookie;
  clientSecret = config.clientSecret;
  clientId = config.clientId;
  apiKey = config.apiKey;
  appBaseUrl = config.appBaseUrl;
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
 * Retries an async function with exponential-ish backoff. Only network errors
 * (thrown exceptions) trigger a retry; HTTP errors and GraphQL errors are
 * returned immediately as ApiResponse.
 */
async function retryWithBackoff<T>(
  fn: () => Promise<T>,
  delays: number[],
): Promise<T> {
  let lastError: unknown;
  for (let i = 0; i <= delays.length; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      if (i < delays.length) {
        await new Promise((resolve) => setTimeout(resolve, delays[i]));
      }
    }
  }
  throw lastError;
}

export type ExecuteOptions = {
  /**
   * Retry delays in ms.
   */
  retries?: number[];
  /**
   * Fetch timeout in ms.
   */
  timeoutMs?: number;
};

/**
 * Runs a GraphQL document against the backend, forwarding the caller's JWT.
 */
export async function executeDocument<T, V = Record<string, unknown>>(
  document: DocumentNode,
  variables?: V,
  authToken?: string,
  options?: ExecuteOptions,
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
  if (clientSecret) {
    headers["X-Client-Secret"] = clientSecret;
    if (clientId) {
      headers["X-Client-Id"] = clientId;
    }
  }
  if (apiKey) {
    headers["X-Api-Key"] = apiKey;
  }
  const clientIp = getRequestIp();
  if (clientIp) {
    headers["X-Forwarded-For"] = clientIp;
  }
  if (appBaseUrl) {
    headers["X-App-Base-Url"] = appBaseUrl;
  }

  const retries = options?.retries ?? [500, 2000, 4000, 6000];
  const timeoutMs = options?.timeoutMs;

  try {
    return await retryWithBackoff(async () => {
      const controller = timeoutMs != null ? new AbortController() : undefined;
      const timeoutId =
        controller && timeoutMs != null
          ? setTimeout(() => controller.abort(), timeoutMs)
          : undefined;
      try {
        const response = await fetch(endpoint, {
          method: "POST",
          headers,
          body: JSON.stringify({ query: print(document), variables }),
          signal: controller?.signal,
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
      } finally {
        if (timeoutId) clearTimeout(timeoutId);
      }
    }, retries);
  } catch (error) {
    const isAbort =
      error instanceof DOMException && error.name === "AbortError";
    let message: string;
    if (isAbort) {
      message = `Timeout after ${timeoutMs}ms`;
    } else if (error instanceof Error) {
      message = error.message;
    } else {
      message = "Unknown error";
    }
    return {
      success: false,
      error: message,
      statusCode: 500,
    };
  }
}
