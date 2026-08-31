import { usePageData } from "@sun/ssr/react";
import { Pagination } from "@sun/components";
import type { HadesTextsQuery } from "~/generated/graphql";

type AttachTextPickerPaginationProps = {
  /**
   * Search query.
   */
  query: string;
  /**
   * Zero-based page index.
   */
  page: number;
  /**
   * Callback for page changes.
   */
  onPageChange: (page: number) => void;
};

/**
 * Renders pagination for the picker. Shares cache key with the items list so it does not blink.
 */
const AttachTextPickerPagination = (props: AttachTextPickerPaginationProps) => {
  const { query, page, onPageChange } = props;

  const pagination = {
    page,
    size: 10,
    filters: query.trim()
      ? [{ field: "title", operator: "MATCHES" as const, value: query.trim() }]
      : undefined,
  };

  const { data } = usePageData<
    NonNullable<HadesTextsQuery["hadesQueries"]["texts"]>
  >("hadesSearch", "blog/hadesSearch", {
    pagination: JSON.stringify(pagination),
  } as unknown as Record<string, string>);
  const pageInfo = data?.pageInfo;

  if (pageInfo == null || pageInfo.totalPages <= 1) {
    return null;
  }

  return (
    <Pagination
      page={page + 1}
      totalPages={pageInfo.totalPages}
      onPageChange={onPageChange}
    />
  );
};

export default AttachTextPickerPagination;
