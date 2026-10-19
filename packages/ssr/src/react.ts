import {
  createElement,
  Fragment,
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "preact/compat";
import { trackPageView } from "@sun/analytics";
import {
  getPageData,
  makeCacheKey,
  refetchEntry,
  subscribeDataInvalidation,
  subscribeDataPatch,
} from "./page-data";
import { MutationError } from "./mutation-error";
import { CSRF_FIELD, getCsrfToken } from "./csrf";

/**
 * Returns a function that forces a re-render.
 *
 * @returns The re-render trigger.
 */
function useForceUpdate(): () => void {
  const [, setVersion] = useState(0);
  return () => setVersion((version) => version + 1);
}

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
  const cacheKey = makeCacheKey(pattern, params);
  const forceUpdate = useForceUpdate();

  useEffect(() => {
    const unsubscribeInvalidation = subscribeDataInvalidation((affected) => {
      // Refresh this entry when it is explicitly affected, or on a blanket
      // invalidation (no keys specified).
      if (!affected || affected.includes(cacheKey)) {
        refetchEntry(key, pattern, params, forceUpdate);
      }
    });
    const unsubscribePatch = subscribeDataPatch((affected) => {
      if (!affected || affected.includes(cacheKey)) {
        forceUpdate();
      }
    });
    return () => {
      unsubscribeInvalidation();
      unsubscribePatch();
    };
  }, [cacheKey]);

  return getPageData<T>(key, pattern, params);
}

/**
 * Sends a pageview for client-side navigations, skipping the first render
 * because the server already captured that pageview.
 *
 * @param pathname Current pathname.
 */
export function usePageViewTracking(pathname: string): void {
  const mounted = useRef(false);
  useEffect(() => {
    if (!mounted.current) {
      mounted.current = true;
      return;
    }
    trackPageView(window.location.href);
  }, [pathname]);
}

type RoleCheckProps = {
  /**
   * Role keys the user must hold, e.g. `["admin"]`.
   */
  roles: string[];
  /**
   * `"all"` - user needs every role. `"any"` - user needs at least one.
   *
   * @default "all"
   */
  match?: "all" | "any";
  children?: ReactNode;
};

/**
 * Renders children only when the current user holds the given roles.
 */
export const RoleCheck = ({ roles, match = "all", children }: RoleCheckProps) => {
  if (!roles.length) return null;
  const { data: userRoles } = usePageData<string[]>("currentRoles", "currentRoles", {});
  if (!userRoles || !userRoles.length) return null;
  const has =
    match === "all"
      ? roles.every((r) => userRoles.includes(r))
      : roles.some((r) => userRoles.includes(r));
  return has ? createElement(Fragment, null, children) : null;
};

/**
 * Hidden form field carrying the CSRF token for native (PRG) form posts.
 */
export function CsrfField() {
  const token = getCsrfToken() ?? "";
  return createElement("input", {
    type: "hidden",
    name: CSRF_FIELD,
    value: token,
    readOnly: true,
  });
}

/**
 * Options for useMutation.
 */
export type UseMutationOptions<TPayload, TResponse, TBase> = {
  /**
   * Current cached value the optimistic state derives from, usually a usePageData result.
   */
  base: TBase;
  /**
   * Folds an optimistic payload into the base value.
   */
  reducer: (current: TBase, payload: TPayload) => TBase;
  /**
   * Server action performing the mutation. Returns the typed response and throws
   * MutationError on failure.
   */
  action: (payload: TPayload) => Promise<TResponse>;
  /**
   * Called after a successful mutation, typically to write the response into the
   * page-data cache with patchPageData so the optimistic state reverts to
   * authoritative data without a refetch.
   */
  onSuccess?: (response: TResponse, payload: TPayload) => void;
  /**
   * Called when the mutation fails. The optimistic state reverts automatically.
   */
  onError?: (error: MutationError, payload: TPayload) => void;
};

/**
 * Applies the reducer immediately, runs the action, and clears the optimistic
 * state once every in-flight mutation settles. onSuccess writes the response
 * into the cache first, so the clear lands on authoritative data.
 *
 * @returns A tuple of the optimistic value, a run function, and the pending flag.
 */
export function useMutation<TPayload, TResponse, TBase = TPayload>(
  options: UseMutationOptions<TPayload, TResponse, TBase>,
): [TBase, (payload: TPayload) => void, boolean] {
  const { base, reducer, action, onSuccess, onError } = options;
  const [optimistic, setOptimistic] = useState<TBase | null>(null);
  const [pending, setPending] = useState(false);
  const inFlight = useRef(0);

  const run = useCallback(
    (payload: TPayload) => {
      inFlight.current += 1;
      setPending(true);
      setOptimistic((current) => reducer(current ?? base, payload));
      void (async () => {
        try {
          const response = await action(payload);
          onSuccess?.(response, payload);
        } catch (error) {
          onError?.(toMutationError(error), payload);
        } finally {
          inFlight.current -= 1;
          if (inFlight.current === 0) {
            setOptimistic(null);
            setPending(false);
          }
        }
      })();
    },
    [base, reducer, action, onSuccess, onError],
  );

  return [optimistic ?? base, run, pending];
}

/**
 * Normalizes an unknown thrown value into a MutationError.
 */
function toMutationError(error: unknown): MutationError {
  if (error instanceof MutationError) {
    return error;
  }
  if (error instanceof Error) {
    return new MutationError(error.message);
  }
  return new MutationError("Mutation failed");
}
