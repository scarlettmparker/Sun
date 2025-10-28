/**
 * Generic API helper for making GraphQL requests.
 * Handles fetching data from the GraphQL server with error handling.
 */

import { ListSongsDocument } from "../generated/graphql";
import { print, DocumentNode } from "graphql";

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: string;
  statusCode?: number;
};

/**
 * Registry of GraphQL operations mapped to their query documents.
 * This allows for easy expansion by adding new operations.
 */
const operationRegistry: Record<string, DocumentNode> = {
  listSongs: ListSongsDocument,
};

/**
 * Registers a new GraphQL operation with its query document.
 * @param operationName The name of the operation.
 * @param queryDocument The GraphQL query document.
 */
export function registerGraphQLOperation(
  operationName: string,
  queryDocument: DocumentNode
): void {
  operationRegistry[operationName] = queryDocument;
}

/**
 * Generic function to fetch data from GraphQL server.
 * @param operationName The name of the GraphQL operation to execute.
 * @param variables Variables for the operation (if any).
 * @returns Promise resolving to ApiResponse.
 */
export async function fetchGraphQLData<T>(
  operationName: string,
  variables?: Record<string, unknown>
): Promise<ApiResponse<T>> {
  try {
    const endpoint =
      process.env.GRAPHQL_ENDPOINT || "http://localhost:8080/graphql";

    const query = operationRegistry[operationName];
    if (!query) {
      return {
        success: false,
        error: "Unknown operation",
        statusCode: 400,
      };
    }

    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        query: print(query),
        variables,
      }),
    });

    if (!response.ok) {
      return {
        success: false,
        error: `HTTP ${response.status}: ${response.statusText}`,
        statusCode: response.status,
      };
    }

    const result = await response.json();

    if (result.errors) {
      return {
        success: false,
        error: result.errors
          .map((e: { message: string }) => e.message)
          .join(", "),
        statusCode: 400,
      };
    }

    return {
      success: true,
      data: result.data,
    };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : "Unknown error",
      statusCode: 500,
    };
  }
}

/**
 * ListSongs operation.
 */
export async function fetchListSongs() {
  return fetchGraphQLData("listSongs");
}
