/**
 * Generic mutation utilities for server-side actions.
 * Provides a registry for mutation handlers and a function to execute mutations.
 */

type MutationHandler = (body: Record<string, unknown>) => Promise<{
  success: boolean;
  data?: unknown;
  error?: string;
  redirect?: string;
}>;

const mutationHandlers: Record<string, MutationHandler> = {};

/**
 * Registers a mutation handler for a specific path.
 * @param path The route path (e.g., 'blog/create').
 * @param handler Function that handles the mutation.
 */
function registerMutationHandler(path: string, handler: MutationHandler): void {
  mutationHandlers[path] = handler;
}

/**
 * Executes a mutation for a given path.
 * @param path The route path.
 * @param body The request body.
 * @returns Promise resolving to the mutation result.
 */
export async function executeMutation(
  path: string,
  body: Record<string, unknown>
): Promise<{
  success: boolean;
  data?: unknown;
  error?: string;
  redirect?: string;
}> {
  const handler = mutationHandlers[path];
  if (!handler) {
    return { success: false, error: "Unknown mutation path" };
  }
  try {
    return await handler(body);
  } catch (error) {
    console.error(`Failed to execute mutation for path ${path}:`, error);
    return { success: false, error: "Internal server error" };
  }
}

/**
 * Mutation registry interface.
 */
interface MutationRegistry {
  registerMutationHandler: (path: string, handler: MutationHandler) => void;
  executeMutation: (
    path: string,
    body: Record<string, unknown>
  ) => Promise<{
    success: boolean;
    data?: unknown;
    error?: string;
    redirect?: string;
  }>;
}

export const mutationRegistry: MutationRegistry = {
  registerMutationHandler,
  executeMutation,
};

/**
 * Clear all mutation handlers (for testing purposes).
 */
export function clearMutationHandlers(): void {
  for (const key in mutationHandlers) {
    delete mutationHandlers[key];
  }
}
