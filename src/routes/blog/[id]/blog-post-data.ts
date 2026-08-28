import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import {
  LocateBlogPostDocument,
  type BlogPost,
  type LocateBlogPostQuery,
} from "~/generated/graphql";

const EMPTY_POST: BlogPost = { id: "", title: "" };

/**
 * Loads a single blog post into the page-data cache.
 */
defineLoader({
  pattern: "blog/:id",
  async loader(params) {
    const id = typeof params.id === "string" ? params.id : "";
    if (!id || id === "create") {
      return { blogPost: EMPTY_POST };
    }
    try {
      const result = await executeDocument<LocateBlogPostQuery>(LocateBlogPostDocument, { id });
      const blogPost = result.success
        ? (result.data as LocateBlogPostQuery | undefined)?.blogQueries?.locateBlogPost
        : null;
      return { blogPost: blogPost ?? EMPTY_POST };
    } catch {
      return { blogPost: EMPTY_POST };
    }
  },
});
