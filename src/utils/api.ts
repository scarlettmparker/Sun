/**
 * GraphQL API helpers, all server-side via executeDocument.
 */

import {
  CreateBlogPostDocument,
  BlogPostInput,
  CreateBlogPostMutation,
  HubRegistryDocument,
  SaveRegistryDocument,
  type HubRegistryInput,
  type HubRegistryQuery,
  type SaveRegistryMutation,
} from "../generated/graphql";
import { executeDocument } from "@sun/api";

/**
 * Create blog post mutation.
 */
export async function mutateCreateBlogPost(
  title: string,
  input: BlogPostInput,
) {
  return executeDocument<CreateBlogPostMutation>(CreateBlogPostDocument, {
    title,
    input,
  });
}

/**
 * Fetches the hub registry.
 */
export async function fetchHubRegistry() {
  return executeDocument<HubRegistryQuery>(HubRegistryDocument);
}

/**
 * Persists the hub registry.
 */
export async function saveHubRegistry(input: HubRegistryInput) {
  return executeDocument<SaveRegistryMutation>(SaveRegistryDocument, { input });
}
