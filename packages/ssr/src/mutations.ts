/**
 * Generic mutation utilities for server-side actions.
 * Provides a registry for mutation handlers and a function to execute mutations.
 */

import { MutationError } from "./mutation-error";

/**
 * Per-request context passed to mutation handlers, so they can forward auth
 * (e.g. extract the app's JWT cookie) to authenticated backend calls.
 */
export type MutationContext = {
  /**
   * Raw Cookie header from the request, for the handler to extract app-specific cookies.
   */
  cookie?: string;
};

export type MutationHandler<TResponse = unknown> = (
  body: Record<string, unknown>,
  context: MutationContext,
) => Promise<TResponse>;

const mutationHandlers: Record<string, MutationHandler<unknown>> = {};

/**
 * Registers a mutation handler for a specific path.
 *
 * @param path The route path (e.g., 'blog/create').
 * @param handler Function that handles the mutation and returns its typed response.
 */
function registerMutationHandler<TResponse>(
  path: string,
  handler: MutationHandler<TResponse>,
): void {
  mutationHandlers[path] = handler as MutationHandler<unknown>;
}

/**
 * Executes a mutation for a given path.
 *
 * @param path The route path.
 * @param body The request body.
 * @param context Per-request context (e.g. the Cookie header) for the handler.
 * @returns Promise resolving to the handler's typed response.
 */
async function executeMutation(
  path: string,
  body: Record<string, unknown>,
  context: MutationContext,
): Promise<unknown> {
  const handler = mutationHandlers[path];
  if (!handler) {
    throw new MutationError(`Unknown mutation path: ${path}`, 404);
  }
  return handler(body, context);
}

/**
 * Mutation registry interface.
 */
interface MutationRegistry {
  registerMutationHandler: typeof registerMutationHandler;
  executeMutation: typeof executeMutation;
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

/**
 * Extracts the variables type from a generated document's __apiType marker
 * (the @graphql-typed-document-node/core convention), falling back to a loose
 * record.
 */
export type VariablesOf<TDoc> = TDoc extends {
  readonly __apiType?: (variables: infer V) => unknown;
}
  ? V
  : Record<string, unknown>;

export interface MutationDefinition<TBody, TResponse = unknown> {
  /**
   * Registered URL path, e.g. "hades/createAnnotation".
   */
  path: string;
  /**
   * Handler that receives the typed request body and returns the typed response.
   */
  handler: (body: TBody, context: MutationContext) => Promise<TResponse>;
}

/**
 * Registers a typed mutation handler. TBody is inferred from the handler's
 * first parameter, so call sites declare the body shape and need no casts.
 */
export function defineMutation<TBody, TResponse = unknown>(
  definition: MutationDefinition<TBody, TResponse>,
): void {
  registerMutationHandler(definition.path, (body, context) =>
    definition.handler(body as TBody, context),
  );
}
