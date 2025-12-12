/**
 * Server actions for blog post operations.
 */

import { QuerySuccess, StandardError } from "~/generated/graphql";

export type MutationResult =
  | QuerySuccess
  | StandardError
  | { __typename: "Redirecting"; redirectTo: string };

/**
 * Executes a server-side mutation by posting to the registered mutation path.
 * @param mutationName The name of the mutation (e.g., 'blog/create').
 * @param body The request body.
 * @returns Promise resolving to the mutation result.
 */
export async function executeMutation(
  mutationName: string,
  body: Record<string, unknown>
): Promise<MutationResult> {
  try {
    const response = await fetch(`/${mutationName}`, {
      method: "POST",
      credentials: "include",
      redirect: "manual",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      return {
        __typename: "StandardError",
        message: `HTTP ${response.status}: ${response.statusText}`,
      };
    }

    const result: MutationResult = await response.json();

    if (result.__typename === "Redirecting") {
      window.location.assign(result.redirectTo);
      return result;
    }

    return result;
  } catch (error) {
    return {
      __typename: "StandardError",
      message: error instanceof Error ? error.message : "Network error",
    };
  }
}
