/**
 * GraphQL API helpers, all server-side via executeDocument.
 */

import {
  AddRemoteObjectDocument,
  AddRemoteObjectMutation,
  type AddRemoteObjectMutationVariables,
  CreateBlogPostDocument,
  BlogPostInput,
  CreateBlogPostMutation,
  DeleteBlogPostDocument,
  type DeleteBlogPostMutation,
  type DeleteBlogPostMutationVariables,
  HadesTextsDocument,
  type HadesTextsQuery,
  type HadesTextsQueryVariables,
  HubRegistryDocument,
  IngestBlogFromSourceDocument,
  type IngestBlogFromSourceMutation,
  type IngestBlogFromSourceMutationVariables,
  IngestBlogInput,
  LocateReaderTextsDocument,
  type LocateReaderTextsQuery,
  type LocateReaderTextsQueryVariables,
  PropertySetDocument,
  type PropertySetQuery,
  type PropertySetQueryVariables,
  RemoveRemoteObjectDocument,
  type RemoveRemoteObjectMutation,
  type RemoveRemoteObjectMutationVariables,
  SaveRegistryDocument,
  WikipediaSummaryDocument,
  type WikipediaSummaryQuery,
  type WikipediaSummaryQueryVariables,
  WiktionaryEntryDocument,
  type WiktionaryEntryQuery,
  type WiktionaryEntryQueryVariables,
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
 * Fetches a wikipedia summary.
 */
export async function fetchWikipediaSummary(title: string) {
  return executeDocument<WikipediaSummaryQuery, WikipediaSummaryQueryVariables>(
    WikipediaSummaryDocument,
    { title },
  );
}

/**
 * Fetches a wiktionary entry.
 */
export async function fetchWiktionaryEntry(word: string) {
  return executeDocument<WiktionaryEntryQuery, WiktionaryEntryQueryVariables>(
    WiktionaryEntryDocument,
    { word },
  );
}

/**
 * Ingests a blog from a source.
 */
export async function mutateIngestBlogFromSource(input: IngestBlogInput) {
  return executeDocument<
    IngestBlogFromSourceMutation,
    IngestBlogFromSourceMutationVariables
  >(IngestBlogFromSourceDocument, { input });
}

/**
 * Adds a remote-object edge to a blog post.
 */
export async function mutateAddRemoteObject(postId: string, target: string) {
  return executeDocument<
    AddRemoteObjectMutation,
    AddRemoteObjectMutationVariables
  >(AddRemoteObjectDocument, { postId, target });
}

/**
 * Removes a remote-object edge from a blog post.
 */
export async function mutateRemoveRemoteObject(postId: string, target: string) {
  return executeDocument<
    RemoveRemoteObjectMutation,
    RemoveRemoteObjectMutationVariables
  >(RemoveRemoteObjectDocument, { postId, target });
}

/**
 * Deletes a blog post and its children.
 */
export async function mutateDeleteBlogPost(id: string) {
  return executeDocument<
    DeleteBlogPostMutation,
    DeleteBlogPostMutationVariables
  >(DeleteBlogPostDocument, { id });
}

/**
 * Locates reader texts by ids in batch.
 */
export async function fetchLocateReaderTexts(ids: string[]) {
  return executeDocument<
    LocateReaderTextsQuery,
    LocateReaderTextsQueryVariables
  >(LocateReaderTextsDocument, { ids });
}

/**
 * Lists hades texts with pagination.
 */
export async function fetchHadesTexts(
  pagination?: HadesTextsQueryVariables["pagination"],
) {
  return executeDocument<HadesTextsQuery, HadesTextsQueryVariables>(
    HadesTextsDocument,
    {
      pagination,
    },
  );
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

/**
 * Fetches a property set's entries as a name-to-values map.
 */
export async function fetchPropertySet(ownerKey: string, name: string) {
  return executeDocument<PropertySetQuery, PropertySetQueryVariables>(
    PropertySetDocument,
    {
      ownerKey,
      name,
    },
    undefined,
    { retries: [], timeoutMs: 2000 },
  );
}
