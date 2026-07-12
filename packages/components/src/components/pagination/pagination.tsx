import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
} from "lucide-react";
import Button from "../button";
import { cn } from "~/utils/cn";
import styles from "./pagination.module.css";

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
 * Pagination renders first/previous controls, a window of page numbers with
 * ellipses, and next/last controls.
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

  const items = pageItems(page, totalPages);
  const atStart = page <= 1;
  const atEnd = page >= totalPages;

  return (
    <nav className={cn(styles.pagination, className)} {...rest}>
      <Button
        variant="secondary"
        aria-label="First page"
        disabled={atStart}
        onClick={() => onPageChange(1)}
      >
        <ChevronsLeft />
      </Button>
      <Button
        variant="secondary"
        aria-label="Previous page"
        disabled={atStart}
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft />
      </Button>
      {items.map((item, i) =>
        item === "ellipsis" ? (
          <span key={`gap-${i}`} className={styles.ellipsis} aria-hidden>
            …
          </span>
        ) : (
          <Button
            key={item}
            variant={item === page ? "default" : "secondary"}
            onClick={() => onPageChange(item)}
          >
            {item}
          </Button>
        ),
      )}
      <Button
        variant="secondary"
        aria-label="Next page"
        disabled={atEnd}
        onClick={() => onPageChange(page + 1)}
      >
        <ChevronRight />
      </Button>
      <Button
        variant="secondary"
        aria-label="Last page"
        disabled={atEnd}
        onClick={() => onPageChange(totalPages)}
      >
        <ChevronsRight />
      </Button>
    </nav>
  );
};

/**
 * Builds the page-number sequence with ellipses: always the first and last
 * page, with a small window around the current page in between.
 */
function pageItems(page: number, totalPages: number): (number | "ellipsis")[] {
  const items: (number | "ellipsis")[] = [];
  const push = (v: number | "ellipsis") => items.push(v);
  const surround = 1;
  const start = Math.max(2, page - surround);
  const end = Math.min(totalPages - 1, page + surround);

  push(1);
  if (start > 2) push("ellipsis");
  for (let p = start; p <= end; p++) push(p);
  if (end < totalPages - 1) push("ellipsis");
  if (totalPages > 1) push(totalPages);
  return items;
}

export default Pagination;
