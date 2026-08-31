import { defineMutation, makeCacheKey, ServerRedirectError } from "@sun/ssr";
import type { MutationResult } from "@sun/ssr";
import {
  mutateAddRemoteObject,
  mutateDeleteBlogPost,
  mutateIngestBlogFromSource,
  mutateRemoveRemoteObject,
} from "~/utils/api";
import { mutateCreateBlogPost } from "~/utils/api";
import type { BlogPostInput, IngestBlogInput } from "~/generated/graphql";

/**
 * Creates a blog post and redirects to it.
 */
defineMutation({
  path: "blog/create",
  async handler(body: Record<string, unknown>) {
    const { title, input } = body;
    const content = (input as BlogPostInput)?.content;

    if (typeof title !== "string" || typeof content !== "string") {
      return {
        __typename: "StandardError",
        message: "Invalid input: title and content must be strings",
      };
    }

    const result = await mutateCreateBlogPost(title, input as BlogPostInput);
    const data = result.data?.blogMutations.createBlogPost as MutationResult;

    if (data?.__typename === "QuerySuccess") {
      const parentId = (input as BlogPostInput)?.parentId as string | undefined;
      const invalidated = [makeCacheKey("blog:blogPosts", {})];
      if (parentId) {
        invalidated.push(makeCacheKey("blog/:id:children", { id: parentId }));
      }
      throw new ServerRedirectError(`/blog/${data.id}`, invalidated, data);
    }

    return {
      __typename: "StandardError",
      message: result.error || "Failed to create blog post",
    };
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
      return { __typename: "StandardError", message: "Invalid input" };
    }
    const result = await mutateAddRemoteObject(postId, target);
    const data = result.data?.blogMutations.addRemoteObject as MutationResult | undefined;
    if (data == null) {
      return { __typename: "StandardError", message: result.error || "Failed" };
    }
    if (data.__typename === "QuerySuccess") {
      return {
        ...data,
        invalidated: [
          makeCacheKey("blog/:id:blogPost", { id: postId }),
          makeCacheKey("blog/gallery:galleryItems", { ids: "*", postId: "*" }),
        ],
      };
    }
    return data;
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
      return { __typename: "StandardError", message: "Invalid input" };
    }
    const result = await mutateRemoveRemoteObject(postId, target);
    const data = result.data?.blogMutations.removeRemoteObject as MutationResult | undefined;
    if (data == null) {
      return { __typename: "StandardError", message: result.error || "Failed" };
    }
    if (data.__typename === "QuerySuccess") {
      return {
        ...data,
        invalidated: [
          makeCacheKey("blog/:id:blogPost", { id: postId }),
          makeCacheKey("blog/gallery:galleryItems", { ids: "*", postId: "*" }),
        ],
      };
    }
    return data;
  },
});

/**
 * Deletes a blog post and its children, then redirects to blog list.
 */
defineMutation({
  path: "blog/delete",
  async handler(body: Record<string, unknown>) {
    const id = body.id as string | undefined;
    if (typeof id !== "string" || !id.trim()) {
      return { __typename: "StandardError", message: "Invalid input" };
    }
    const result = await mutateDeleteBlogPost(id);
    const data = result.data?.blogMutations.deleteBlogPost as MutationResult | undefined;
    if (data == null) {
      return { __typename: "StandardError", message: result.error || "Failed" };
    }
    if (data.__typename === "QuerySuccess") {
      throw new ServerRedirectError("/blog", [makeCacheKey("blog:blogPosts", {})], data);
    }
    return data;
  },
});

/**
 * Ingests a blog from wikipedia or wiktionary.
 */
defineMutation({
  path: "blog/ingest-source",
  async handler(body: Record<string, unknown>) {
    const input = body.input as IngestBlogInput | undefined;
    if (input == null || typeof input.title !== "string" || typeof input.sourceId !== "string") {
      return { __typename: "StandardError", message: "Invalid input" };
    }
    const result = await mutateIngestBlogFromSource(input);
    const data = result.data?.blogMutations.ingestBlogFromSource as MutationResult | undefined;
    if (data == null) {
      return { __typename: "StandardError", message: result.error || "Failed" };
    }
    if (data.__typename === "QuerySuccess" && data.id) {
      const parentId = (input as IngestBlogInput)?.parentId as string | undefined;
      const invalidated = [
        makeCacheKey("blog:blogPosts", {}),
        makeCacheKey("home:home", {}),
        makeCacheKey("blog/:id:blogPost", { id: data.id }),
      ];
      if (parentId) {
        invalidated.push(makeCacheKey("blog/:id:children", { id: parentId }));
        invalidated.push(makeCacheKey("blog/:id:blogPost", { id: parentId }));
      }
      throw new ServerRedirectError(`/blog/${data.id}`, invalidated, data);
    }
    return data;
  },
});
