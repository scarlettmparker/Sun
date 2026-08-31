import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import {
  ListBlogPostsByRemoteObjectsDocument,
  type ListBlogPostsByRemoteObjectsQuery,
} from "~/generated/graphql";

/**
 * Loads related blog posts by remoteObject ids.
 */
defineLoader({
  pattern: "blog/relatedPosts",
  async loader(params) {
    const rawIds = (params as Record<string, unknown>).ids;
    let ids: string[] = [];
    if (Array.isArray(rawIds)) {
      ids = rawIds as string[];
    } else if (typeof rawIds === "string") {
      ids = JSON.parse(rawIds) as string[];
    }
    if (!ids.length) {
      return {
        relatedPosts: [] as NonNullable<
          ListBlogPostsByRemoteObjectsQuery["blogQueries"]["listByRemoteObjects"]
        >,
      };
    }
    try {
      const result = await executeDocument<ListBlogPostsByRemoteObjectsQuery>(
        ListBlogPostsByRemoteObjectsDocument,
        { ids },
      );
      const relatedPosts = result.success
        ? ((result.data as ListBlogPostsByRemoteObjectsQuery | undefined)
            ?.blogQueries?.listByRemoteObjects ?? [])
        : [];
      return { relatedPosts: relatedPosts ?? [] };
    } catch {
      return {
        relatedPosts: [] as NonNullable<
          ListBlogPostsByRemoteObjectsQuery["blogQueries"]["listByRemoteObjects"]
        >,
      };
    }
  },
});
