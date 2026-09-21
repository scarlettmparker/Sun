import { getCsrfToken, CSRF_HEADER } from "./csrf";
import { MutationError } from "./mutation-error";

/**
 * Executes a server-side mutation by posting to the registered mutation path.
 *
 * @param mutationName The mutation path (e.g. "blog/update").
 * @param body The request body.
 * @returns Promise resolving to the typed response, or rejecting with MutationError.
 */
export async function executeMutation<T = unknown>(
  mutationName: string,
  body: Record<string, unknown>,
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/${mutationName}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [CSRF_HEADER]: getCsrfToken() ?? "",
      },
      body: JSON.stringify(body),
    });
  } catch (error) {
    throw new MutationError(
      error instanceof Error ? error.message : "Network error",
    );
  }

  if (!response.ok) {
    throw new MutationError(await readErrorMessage(response), response.status);
  }

  return (await response.json()) as T;
}

/**
 * Reads the error message from a failed mutation response.
 *
 * @param response The failed HTTP response.
 */
async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json();
    if (body && typeof body === "object" && "message" in body) {
      return String((body as { message: unknown }).message);
    }
  } catch {
    // fall through to generic
  }
  return `HTTP ${response.status}: ${response.statusText}`;
}
