import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import { ListBlogPostsDocument, type ListBlogPostsQuery } from "~/generated/graphql";

/**
 * Loads the blog post list into the page-data cache.
 */
defineLoader({
  pattern: "blog",
  async loader(params) {
    const page = Number((params as Record<string, unknown>).page ?? 0);
    const type = typeof (params as Record<string, unknown>).type === "string"
      ? ((params as Record<string, unknown>).type as string)
      : undefined;
    const filters = type
      ? [{ field: "type.name", operator: "EQUALS" as const, value: type }]
      : [];
    try {
      const result = await executeDocument<ListBlogPostsQuery>(ListBlogPostsDocument, {
        pagination: { page, size: 50, filters, sortBy: "createdAt", sortDir: "DESC" as const },
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
