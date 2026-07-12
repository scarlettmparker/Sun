/**
 * Generic mutation utilities for server-side actions.
 * Provides a registry for mutation handlers and a function to execute mutations.
 */

import { MutationResult } from "./client-mutation";
import { ServerRedirectError } from "./server-redirect";

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

export type MutationHandler = (
  body: Record<string, unknown>,
  context: MutationContext,
) => Promise<MutationResult>;

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
 * @param context Per-request context (e.g. the Cookie header) for the handler.
 * @returns Promise resolving to the mutation result.
 */
async function executeMutation(
  path: string,
  body: Record<string, unknown>,
  context: MutationContext,
): Promise<MutationResult> {
  const handler = mutationHandlers[path];
  if (!handler) {
    return { __typename: "StandardError", message: "Unknown mutation path" };
  }
  try {
    return await handler(body, context);
  } catch (error) {
    if (error instanceof ServerRedirectError) {
      throw error;
    } // Only catch and log genuine internal errors

    console.error(`Failed to execute mutation for path ${path}:`, error);
    return { __typename: "StandardError", message: "Internal server error" };
  }
}

/**
 * Mutation registry interface.
 */
interface MutationRegistry {
  registerMutationHandler: (path: string, handler: MutationHandler) => void;
  executeMutation: (
    path: string,
    body: Record<string, unknown>,
    context: MutationContext,
  ) => Promise<MutationResult>;
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

export interface TypedDocumentNode<TResult, TVariables> {
  /**
   * Marker carrying the inferred result type.
   */
  readonly __result?: TResult;
  /**
   * Marker carrying the inferred variables type.
   */
  readonly __variables?: TVariables;
}

export interface MutationDefinition<TDoc extends TypedDocumentNode<unknown, unknown>> {
  /**
   * Registered URL path, e.g. "hades/createAnnotation".
   */
  path: string;
  /**
   * Generated document supplying the variable and result types.
   */
  document: TDoc;
  /**
   * Handler that receives typed variables and returns the mutation result.
   */
  handler: (
    variables: TDoc extends TypedDocumentNode<unknown, infer V> ? V : never,
    context: MutationContext,
  ) => Promise<MutationResult>;
}

/**
 * Registers a typed mutation handler.
 */
export function defineMutation<TDoc extends TypedDocumentNode<unknown, unknown>>(
  definition: MutationDefinition<TDoc>,
): MutationDefinition<TDoc> {
  registerMutationHandler(definition.path, async (body, context) =>
    definition.handler(body as never, context),
  );
  return definition;
}
