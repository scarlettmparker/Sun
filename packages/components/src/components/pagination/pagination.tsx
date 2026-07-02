import Button from "../button";
import { cn } from "~/utils/cn";
import "./pagination.module.css";

type PaginationProps = {
  /**
   * Current page, 1-based.
   */
  page: number;
  /**
   * Total number of pages.
   */
  totalPages: number;
  /**
   * Called with the newly selected page.
   */
  onPageChange: (page: number) => void;
} & Omit<React.HTMLAttributes<HTMLElement>, "children">;

/**
 * Pagination renders previous/next controls and a window of page numbers.
 */
const Pagination = ({
  page,
  totalPages,
  onPageChange,
  className,
  ...rest
}: PaginationProps) => {
  if (totalPages <= 1) {
    return null;
  }

  const pages = pageWindow(page, totalPages);

  return (
    <nav className={cn("pagination", className)} {...rest}>
      <Button
        variant="secondary"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
      >
        Previous
      </Button>
      {pages.map((p) => (
        <Button
          key={p}
          variant={p === page ? "default" : "secondary"}
          onClick={() => onPageChange(p)}
        >
          {p}
        </Button>
      ))}
      <Button
        variant="secondary"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        Next
      </Button>
    </nav>
  );
};

/**
 * Returns up to 5 contiguous page numbers centred on the current page.
 */
function pageWindow(page: number, totalPages: number): number[] {
  const max = 5;
  if (totalPages <= max) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }
  let start = Math.max(1, page - 2);
  const end = Math.min(totalPages, start + max - 1);
  start = Math.max(1, end - max + 1);
  return Array.from({ length: end - start + 1 }, (_, i) => start + i);
}

export default Pagination;
