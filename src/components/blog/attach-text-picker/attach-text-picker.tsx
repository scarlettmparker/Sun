import { Suspense, useState, useTransition } from "react";
import { useTranslation } from "react-i18next";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  SearchBar,
} from "@sun/components";
import AttachTextPickerItems from "~/components/blog/attach-text-picker-items";
import AttachTextPickerPagination from "~/components/blog/attach-text-picker-pagination";
import styles from "./attach-text-picker.module.css";
import AttachTextPickerItemsSkeleton from "./skeletons/attach-text-picker-items-skeleton";

type AttachTextPickerProps = {
  /**
   * Post id to attach to.
   */
  postId: string;
};

/**
 * Searchable picker to attach a hades text to a blog post.
 */
const AttachTextPicker = (props: AttachTextPickerProps) => {
  const { postId } = props;
  const { t } = useTranslation("blog");
  const [query, setQuery] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [page, setPage] = useState(0);
  const [, startTransition] = useTransition();

  const handleSearch = (value: string) => {
    setQuery(value);
    setPage(0);
  };

  const handlePageChange = (newPage: number) => {
    startTransition(() => {
      setPage(newPage - 1);
    });
  };

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>{t("attach.title")}</CardTitle>
        </CardHeader>
        <CardBody>
          <div className={styles.inner}>
            <div className={styles.toolbar}>
              <SearchBar
                value={searchInput}
                onChange={setSearchInput}
                onSearch={handleSearch}
                placeholder={t("attach.placeholder")}
              />
            </div>
            <Suspense fallback={<AttachTextPickerItemsSkeleton />}>
              <AttachTextPickerItems
                postId={postId}
                query={query}
                page={page}
              />
            </Suspense>
          </div>
        </CardBody>
      </Card>
      <Suspense fallback={null}>
        <AttachTextPickerPagination
          query={query}
          page={page}
          onPageChange={handlePageChange}
        />
      </Suspense>
    </>
  );
};

export default AttachTextPicker;
