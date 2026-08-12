/**
 * GraphQL API helpers, all server-side via executeDocument.
 */

import {
  ListSongsDocument,
  LocateSongDocument,
  ListBlogPostsDocument,
  LocateBlogPostDocument,
  CreateBlogPostDocument,
  BlogPostInput,
  CreateBlogPostMutation,
  ListGalleryItemsDocument,
  ListGalleryItemsByRemoteObjectsDocument,
  HubRegistryDocument,
  SaveRegistryDocument,
  type HubRegistryInput,
  type HubRegistryQuery,
  type SaveRegistryMutation,
} from "../generated/graphql";
import { executeDocument } from "@sun/api";

export { executeDocument } from "@sun/api";
export type { ApiResponse } from "@sun/api";

/**
 * List operation for blog posts.
 */
export async function fetchListBlogPosts() {
  return executeDocument(ListBlogPostsDocument);
}

/**
 * Locate operation for blog posts.
 */
export async function fetchLocateBlogPost(id: string) {
  return executeDocument(LocateBlogPostDocument, { id });
}

/**
 * List operation.
 */
export async function fetchListSongs() {
  return executeDocument(ListSongsDocument);
}

/**
 * Locate operation for songs.
 */
export async function fetchLocateSong(id: string) {
  return executeDocument(LocateSongDocument, { id });
}

/**
 * List gallery items.
 */
export async function fetchListGalleryItems() {
  return executeDocument(ListGalleryItemsDocument);
}

/**
 * List gallery items by foreign objects.
 */
export async function fetchListGalleryItemsByRemoteObjects(ids: string[]) {
  return executeDocument(ListGalleryItemsByRemoteObjectsDocument, { ids });
}

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
