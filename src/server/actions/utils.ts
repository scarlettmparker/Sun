/**
 * Server actions for blog post operations.
 */

import { QuerySuccess, StandardError } from "~/generated/graphql";

export type MutationResult = QuerySuccess | StandardError;

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
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      return {
        message: `HTTP ${response.status}: ${response.statusText}`,
      };
    }

    const result: MutationResult = await response.json();
    return result;
  } catch (error) {
    return {
      message: error instanceof Error ? error.message : "Network error",
    };
  }
}
