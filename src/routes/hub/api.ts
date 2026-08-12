import { useEffect, useState } from "react";
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
 * Subscribes to the status stream, filling in each app as it resolves.
 */
export function useHubStatusStream(): Record<string, AppRuntimeStatus> {
  const [statuses, setStatuses] = useState<Record<string, AppRuntimeStatus>>(
    {},
  );

  useEffect(() => {
    const source = new EventSource("/hub/api/status/stream");
    source.onopen = () => setStatuses({});

    source.addEventListener("status", (event) => {
      const status = JSON.parse(event.data) as AppRuntimeStatus;
      setStatuses((previous) => ({ ...previous, [status.key]: status }));
    });

    source.addEventListener("remove", (event) => {
      const key = (JSON.parse(event.data) as { key: string }).key;
      setStatuses((previous) => {
        const next = { ...previous };
        delete next[key];
        return next;
      });
    });

    return () => source.close();
  }, []);

  return statuses;
}
