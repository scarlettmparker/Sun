import { createElement, useEffect, useReducer, useState } from "react";
import {
  getPageData,
  makeCacheKey,
  refetchEntry,
  subscribeDataInvalidation,
} from "./page-data";
import { CSRF_FIELD, getCsrfToken } from "./csrf";

/**
 * Suspense-aware page-data hook.
 *
 * @param key Data key to read (e.g. "keys").
 * @param pattern Route pattern (e.g. "bucket/:alias/*").
 * @param params Parameters for the data loader.
 */
export function usePageData<T>(
  key: string,
  pattern: string,
  params?: Record<string, unknown>,
): { data: T } {
  const cacheKey = makeCacheKey(`${pattern}:${key}`, params);
  const [, forceUpdate] = useReducer((x: number) => x + 1, 0);

  useEffect(() => {
    return subscribeDataInvalidation((affected) => {
      // Refresh this entry when it is explicitly affected, or on a blanket
      // invalidation (no keys specified).
      if (!affected || affected.includes(cacheKey)) {
        refetchEntry(key, pattern, params, forceUpdate);
      }
    });
  }, [cacheKey]);

  return getPageData<T>(key, pattern, params);
}

/**
 * Hidden form field carrying the CSRF token for native (PRG) form posts.
 */
export function CsrfField() {
  const [token, setToken] = useState("");
  useEffect(() => {
    setToken(getCsrfToken() ?? "");
  }, []);
  return createElement("input", {
    type: "hidden",
    name: CSRF_FIELD,
    value: token,
  });
}
