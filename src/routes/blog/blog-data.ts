import { defineLoader } from "@sun/ssr";
import { fetchListBlogPosts } from "~/utils/api";
import type { ListBlogPostsQuery } from "~/generated/graphql";

/**
 * Loads the blog post list into the page-data cache.
 */
defineLoader({
  pattern: "blog",
  async loader() {
    try {
      const result = await fetchListBlogPosts();
      const blogPosts = result.success
        ? (result.data as ListBlogPostsQuery | undefined)?.blogQueries?.listBlogPosts?.items
        : null;
      return { blogPosts: blogPosts ?? [] };
    } catch {
      return { blogPosts: [] };
    }
  },
});
