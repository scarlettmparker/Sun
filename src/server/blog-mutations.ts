import { defineMutation, MutationError } from "@sun/ssr";
import {
  mutateAddRemoteObject,
  mutateCreateBlogPost,
  mutateDeleteBlogPost,
  mutateIngestBlogFromSource,
  mutateRemoveRemoteObject,
} from "~/utils/api";
import type { BlogPostInput, IngestBlogInput } from "~/generated/graphql";

/**
 * Creates a blog post and returns it.
 */
defineMutation({
  path: "blog/create",
  async handler(body: Record<string, unknown>) {
    const { title, input } = body;
    const content = (input as BlogPostInput)?.content;

    if (typeof title !== "string" || typeof content !== "string") {
      throw new MutationError("Invalid input: title and content must be strings");
    }

    const result = await mutateCreateBlogPost(title, input as BlogPostInput);
    const response = result.data?.blogMutations.createBlogPost;
    if (response == null) {
      throw new MutationError(result.error ?? "Failed to create blog post");
    }
    return response;
  },
});

/**
 * Adds a remote object edge to a post.
 */
defineMutation({
  path: "blog/add-remote-object",
  async handler(body: Record<string, unknown>) {
    const postId = body.postId as string | undefined;
    const target = body.target as string | undefined;
    if (typeof postId !== "string" || typeof target !== "string") {
      throw new MutationError("Invalid input");
    }
    const result = await mutateAddRemoteObject(postId, target);
    const response = result.data?.blogMutations.addRemoteObject;
    if (response == null) {
      throw new MutationError(result.error ?? "Failed to add remote object");
    }
    return response;
  },
});

/**
 * Removes a remote object edge from a post.
 */
defineMutation({
  path: "blog/remove-remote-object",
  async handler(body: Record<string, unknown>) {
    const postId = body.postId as string | undefined;
    const target = body.target as string | undefined;
    if (typeof postId !== "string" || typeof target !== "string") {
      throw new MutationError("Invalid input");
    }
    const result = await mutateRemoveRemoteObject(postId, target);
    const response = result.data?.blogMutations.removeRemoteObject;
    if (response == null) {
      throw new MutationError(result.error ?? "Failed to remove remote object");
    }
    return response;
  },
});

/**
 * Deletes a blog post and its children.
 */
defineMutation({
  path: "blog/delete",
  async handler(body: Record<string, unknown>) {
    const id = body.id as string | undefined;
    if (typeof id !== "string" || !id.trim()) {
      throw new MutationError("Invalid input");
    }
    const result = await mutateDeleteBlogPost(id);
    const response = result.data?.blogMutations.deleteBlogPost;
    if (response == null) {
      throw new MutationError(result.error ?? "Failed to delete blog post");
    }
    return response;
  },
});

/**
 * Ingests a blog from wikipedia or wiktionary.
 */
defineMutation({
  path: "blog/ingest-source",
  async handler(body: Record<string, unknown>) {
    const input = body.input as IngestBlogInput | undefined;
    if (
      input == null ||
      typeof input.title !== "string" ||
      typeof input.sourceId !== "string"
    ) {
      throw new MutationError("Invalid input");
    }
    const result = await mutateIngestBlogFromSource(input);
    const response = result.data?.blogMutations.ingestBlogFromSource;
    if (response == null) {
      throw new MutationError(result.error ?? "Failed to ingest blog");
    }
    return response;
  },
});
