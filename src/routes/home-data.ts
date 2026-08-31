import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import {
  BlogPostTypesDocument,
  type BlogPostTypesQuery,
} from "~/generated/graphql";

/**
 * Loads blog post types for the home hub.
 */
defineLoader({
  pattern: "home",
  async loader() {
    try {
      const result = await executeDocument<BlogPostTypesQuery>(
        BlogPostTypesDocument,
        {},
      );
      const types = result.success
        ? ((result.data as BlogPostTypesQuery | undefined)?.blogQueries
            .blogPostTypes ?? [])
        : [];
      return { types };
    } catch {
      return {
        types: [] as BlogPostTypesQuery["blogQueries"]["blogPostTypes"],
      };
    }
  },
});
