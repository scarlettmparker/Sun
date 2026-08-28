import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import {
  ListBlogPostsDocument,
  type ListBlogPostsQuery,
  type PaginationInput,
} from "~/generated/graphql";

/**
 * Loads the blog post list into the page-data cache.
 */
defineLoader({
  pattern: "blog",
  async loader(params) {
    const pagination = (params as { pagination?: PaginationInput }).pagination;
    try {
      const result = await executeDocument<ListBlogPostsQuery>(ListBlogPostsDocument, {
        pagination,
      });
      const payload = result.success
        ? (result.data as ListBlogPostsQuery | undefined)?.blogQueries?.listBlogPosts
        : null;
      return { blogPosts: payload?.items ?? [] };
    } catch {
      return { blogPosts: [] };
    }
  },
});
