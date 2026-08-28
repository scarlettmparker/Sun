import { defineMutation, makeCacheKey, ServerRedirectError } from "@sun/ssr";
import type { MutationResult } from "@sun/ssr";
import { mutateCreateBlogPost } from "~/utils/api";
import type { BlogPostInput } from "~/generated/graphql";

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
      throw new ServerRedirectError(
        `/blog/${data.id}`,
        makeCacheKey("blog:blogPosts", {}),
        data,
      );
    }

    return {
      __typename: "StandardError",
      message: result.error || "Failed to create blog post",
    };
  },
});
