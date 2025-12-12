/**
 * Generic API helper for making GraphQL requests.
 * Handles fetching data from the GraphQL server with error handling.
 */

import {
  ListSongsDocument,
  LocateSongDocument,
  ListBlogPostsDocument,
  LocateBlogPostDocument,
  CreateBlogPostDocument,
  BlogPostInput,
  CreateBlogPostMutation,
} from "../generated/graphql";
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
  blogQueries: {
    listBlogPosts: DocumentNode;
    locateBlogPost: DocumentNode;
  };
  blogMutations: {
    createBlogPost: DocumentNode;
  };
};

/**
 * Registry of GraphQL operations mapped to their query documents.
 */
const operationRegistry: OperationRegistry = {
  songQueries: {
    list: ListSongsDocument,
    locate: LocateSongDocument,
  },
  blogQueries: {
    listBlogPosts: ListBlogPostsDocument,
    locateBlogPost: LocateBlogPostDocument,
  },
  blogMutations: {
    createBlogPost: CreateBlogPostDocument,
  },
};

/**
 * Retrieves a GraphQL operation document by its namespaced path.
 *
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
 *
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
 * Retry with backoff function.
 */
const retryWithBackoff = async <T>(
  fn: () => Promise<T>,
  delays: number[]
): Promise<T> => {
  let lastError: unknown;
  for (let i = 0; i <= delays.length; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      if (i < delays.length) {
        await new Promise((resolve) => setTimeout(resolve, delays[i]));
      }
    }
  }
  throw lastError;
};

/**
 * Generic function to fetch data from GraphQL server.
 *
 * @param operationName The name of the GraphQL operation to execute.
 * @param variables Variables for the operation (if any).
 * @returns Promise resolving to ApiResponse.
 */
export async function fetchGraphQLData<T>(
  operationName: string,
  variables?: Record<string, unknown>
): Promise<ApiResponse<T>> {
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

  try {
    return await retryWithBackoff(async () => {
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
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();

      if (result.errors) {
        throw new Error(
          result.errors.map((e: { message: string }) => e.message).join(", ")
        );
      }

      return {
        success: true,
        data: result.data,
      };
    }, [500, 2000, 4000, 6000]);
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : "Unknown error",
      statusCode: 500,
    };
  }
}

/**
 * List operation for blog posts.
 */
export async function fetchListBlogPosts() {
  return fetchGraphQLData("blogQueries.listBlogPosts");
}

/**
 * Locate operation for blog posts.
 */
export async function fetchLocateBlogPost(id: string) {
  return fetchGraphQLData("blogQueries.locateBlogPost", { id });
}

/**
 * List operation.
 */
export async function fetchListSongs() {
  return fetchGraphQLData("songQueries.list");
}

/**
 * Locate operation for songs.
 */
export async function fetchLocateSong(id: string) {
  return fetchGraphQLData("songQueries.locate", { id });
}

/**
 * Create blog post mutation.
 */
export async function mutateCreateBlogPost(
  title: string,
  input: BlogPostInput
) {
  return fetchGraphQLData<CreateBlogPostMutation>(
    "blogMutations.createBlogPost",
    { title, input }
  );
}
