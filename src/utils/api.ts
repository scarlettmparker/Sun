/**
 * Generic API helper for making GraphQL requests.
 * Handles fetching data from the GraphQL server with error handling.
 */

import { ListSongsDocument, LocateSongDocument } from "../generated/graphql";
import { print, DocumentNode } from "graphql";

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: string;
  statusCode?: number;
};

/**
 * Type definition for the operation registry with strong typing.
 */
type OperationRegistry = {
  songQueries: {
    list: DocumentNode;
    locate: DocumentNode;
  };
};

/**
 * Registry of GraphQL operations mapped to their query documents.
 * Supports namespaced operations using dot notation
 */
const operationRegistry: OperationRegistry = {
  songQueries: {
    list: ListSongsDocument,
    locate: LocateSongDocument,
  },
};

/**
 * Retrieves a GraphQL operation document by its namespaced path.
 * @param path The dot-separated path to the operation
 * @returns The DocumentNode if found, otherwise undefined.
 */
function getOperation(path: string): DocumentNode | undefined {
  const parts = path.split(".");
  let current: unknown = operationRegistry;
  for (const part of parts) {
    if (
      current &&
      typeof current === "object" &&
      current !== null &&
      part in current
    ) {
      current = (current as Record<string, unknown>)[part];
    } else {
      return undefined;
    }
  }
  return current as DocumentNode;
}

/**
 * Registers a new GraphQL operation with its query document.
 * Note: This function assumes flat registration; for namespaced operations,
 * update the registry directly or extend this function to handle paths.
 * @param operationName The name of the operation.
 * @param queryDocument The GraphQL query document.
 */
export function registerGraphQLOperation(
  operationName: string,
  queryDocument: DocumentNode
): void {
  (operationRegistry as Record<string, unknown>)[operationName] = queryDocument;
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

    const query = getOperation(operationName);
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
 * List operation for songs.
 */
export async function fetchList() {
  return fetchGraphQLData("songQueries.list");
}

/**
 * Locate operation for songs.
 */
export async function fetchLocate(id: string) {
  return fetchGraphQLData("songQueries.locate", { id });
}
