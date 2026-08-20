/**
 * Server actions for blog post operations.
 */

import { revalidatePageData } from "./page-data";
import { getCsrfToken, CSRF_HEADER } from "./csrf";

export type BaseMutationResult =
  | { __typename: "QuerySuccess"; id?: string | null; message: string }
  | { __typename: "StandardError"; message: string }
  | { __typename: "FormError"; message: string }
  | { __typename: "Redirect"; redirectTo: string };

export type MutationResult = BaseMutationResult & {
  /**
   * Cache-key patterns the handler invalidated server-side. The client mirrors
   * this on its own read-through cache and refetches via /__page-data, avoiding
   * a full route reload.
   */
  invalidated?: string[];
};

/**
 * Executes a server-side mutation by posting to the registered mutation path.
 * @param mutationName The name of the mutation (e.g., 'blog/create').
 * @param body The request body.
 * @returns Promise resolving to the mutation result.
 */
export async function executeMutation(
  mutationName: string,
  body: Record<string, unknown>,
): Promise<MutationResult> {
  try {
    const response = await fetch(`/${mutationName}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [CSRF_HEADER]: getCsrfToken() ?? "",
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      try {
        const body = await response.json();
        if (body && typeof body === "object" && "message" in body) {
          return {
            __typename: "StandardError",
            message: (body as { message: string }).message,
          };
        }
      } catch {
        // fall through to generic
      }
      return {
        __typename: "StandardError",
        message: `HTTP ${response.status}: ${response.statusText}`,
      };
    }

    const result: MutationResult = await response.json();

    if (result.invalidated && result.invalidated.length) {
      revalidatePageData(result.invalidated);
    }

    return result;
  } catch (error) {
    return {
      __typename: "StandardError",
      message: error instanceof Error ? error.message : "Network error",
    };
  }
}
