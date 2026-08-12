import type { AppRuntimeStatus } from "~/server/hub/types";

const TOKEN_KEY = "hubAdminToken";

/**
 * Reads the stored admin token.
 */
export function getHubToken(): string {
  if (typeof window === "undefined") {
    return "";
  }
  return window.localStorage.getItem(TOKEN_KEY) ?? "";
}

/**
 * Stores the admin token.
 */
export function setHubToken(token: string): void {
  window.localStorage.setItem(TOKEN_KEY, token);
}

/**
 * Fetches the live per-app statuses.
 */
export async function fetchHubStatuses(): Promise<AppRuntimeStatus[]> {
  const response = await fetch("/hub/api/status");
  if (!response.ok) {
    return [];
  }
  return (await response.json()) as AppRuntimeStatus[];
}
