import {
  createElement,
  Fragment,
  type ReactNode,
  useCallback,
  useEffect,
  useOptimistic,
  useReducer,
  useTransition,
} from "react";
import {
  getPageData,
  makeCacheKey,
  peekPageData,
  refetchEntry,
  subscribeDataInvalidation,
  subscribeDataPatch,
} from "./page-data";
import { MutationError } from "./mutation-error";
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
export const RoleCheck = ({
  roles,
  match = "all",
  children,
}: RoleCheckProps) => {
  if (!roles.length) return null;
  const cacheKey = makeCacheKey("currentRoles:currentRoles", {});
  const [, forceUpdate] = useReducer((x: number) => x + 1, 0);

  useEffect(() => {
    return subscribeDataInvalidation((affected) => {
      if (!affected || affected.includes(cacheKey)) {
        forceUpdate();
      }
    });
  }, [cacheKey]);

  const userRoles = peekPageData<string[]>(
    "currentRoles",
    "currentRoles",
    {},
  ) as string[] | null;

  useEffect(() => {
    if (userRoles == null) {
      refetchEntry("currentRoles", "currentRoles", {}, forceUpdate);
    }
  }, [userRoles == null]);

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
 * Wraps a mutation in React's useOptimistic: the reducer applies immediately, the
 * action runs in a transition, and the optimistic state reverts once the
 * transition settles. onSuccess writes the response into the cache first, so the
 * revert lands on authoritative data.
 *
 * @returns A tuple of the optimistic value, a run function, and the pending flag.
 */
export function useMutation<TPayload, TResponse, TBase = TPayload>(
  options: UseMutationOptions<TPayload, TResponse, TBase>,
): [TBase, (payload: TPayload) => void, boolean] {
  const { base, reducer, action, onSuccess, onError } = options;
  const [optimistic, dispatchOptimistic] = useOptimistic(base, reducer);
  const [pending, startTransition] = useTransition();

  const run = useCallback(
    (payload: TPayload) => {
      startTransition(async () => {
        dispatchOptimistic(payload);
        try {
          const response = await action(payload);
          onSuccess?.(response, payload);
        } catch (error) {
          onError?.(toMutationError(error), payload);
        }
      });
    },
    [action, onSuccess, onError, dispatchOptimistic],
  );

  return [optimistic, run, pending];
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
