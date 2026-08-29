import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import { HadesTextsDocument, type HadesTextsQuery, type HadesTextsQueryVariables } from "~/generated/graphql";

/**
 * Loads hades texts for attach search.
 */
defineLoader({
  pattern: "blog/hadesSearch",
  async loader(params) {
    const rawPagination = (params as Record<string, unknown>).pagination;
    let pagination: HadesTextsQueryVariables["pagination"] | undefined;
    if (typeof rawPagination === "string") {
      try {
        pagination = JSON.parse(rawPagination) as HadesTextsQueryVariables["pagination"];
      } catch {
        pagination = undefined;
      }
    } else if (rawPagination != null && typeof rawPagination === "object") {
      pagination = rawPagination as HadesTextsQueryVariables["pagination"];
    }
    try {
      const result = await executeDocument<HadesTextsQuery, HadesTextsQueryVariables>(HadesTextsDocument, {
        pagination,
      });
      const payload = result.success
        ? (result.data as HadesTextsQuery | undefined)?.hadesQueries?.texts
        : null;
      return {
        hadesSearch: payload ?? { items: [], pageInfo: { page: 0, size: 0, totalPages: 0, totalCount: 0, hasNextPage: false, hasPreviousPage: false } },
      };
    } catch {
      return {
        hadesSearch: { items: [], pageInfo: { page: 0, size: 0, totalPages: 0, totalCount: 0, hasNextPage: false, hasPreviousPage: false } },
      };
    }
  },
});
