import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import { ChildrenDocument, type ChildrenQuery } from "~/generated/graphql";

const EMPTY_CHILDREN: ChildrenQuery["blogQueries"]["children"] = {
  items: [],
  pageInfo: {
    page: 0,
    size: 0,
    totalPages: 0,
    totalCount: 0,
    hasNextPage: false,
    hasPreviousPage: false,
  },
};

/**
 * Loads children for a blog post.
 */
defineLoader({
  pattern: "blog/:id/children",
  async loader(params) {
    const id = typeof params.id === "string" ? params.id : "";
    if (!id || id === "create") {
      return { children: EMPTY_CHILDREN };
    }
    try {
      const result = await executeDocument<ChildrenQuery>(ChildrenDocument, {
        parentId: id,
        pagination: { page: 0, size: 50, sortBy: "createdAt", sortDir: "DESC" as const },
      });
      const children = result.success
        ? (result.data as ChildrenQuery | undefined)?.blogQueries?.children
        : null;
      return { children: children ?? EMPTY_CHILDREN };
    } catch {
      return { children: EMPTY_CHILDREN };
    }
  },
});
