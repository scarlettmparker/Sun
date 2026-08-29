import { useState, useTransition } from "react";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Button, Card, CardBody, CardHeader, CardTitle, Pagination, SearchBar } from "@sun/components";
import { PlusIcon } from "@heroicons/react/24/outline";
import { attachRemoteObject } from "~/server/actions/blog-post";
import type { HadesTextsQuery } from "~/generated/graphql";
import styles from "./attach-text-dialog.module.css";

type AttachTextDialogProps = {
  /**
   * Post id to attach to.
   */
  postId: string;
};

/**
 * Searchable picker to attach a hades text to a blog post.
 */
const AttachTextDialog = (props: AttachTextDialogProps) => {
  const { postId } = props;
  const { t } = useTranslation("blog");
  const [query, setQuery] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [page, setPage] = useState(0);
  const [isPending, startTransition] = useTransition();

  const handleSearch = (value: string) => {
    setQuery(value);
    setPage(0);
  };

  const handlePageChange = (newPage: number) => {
    startTransition(() => {
      setPage(newPage - 1);
    });
  };

  const pagination = {
    page,
    size: 10,
    filters: query.trim() ? [{ field: "title", operator: "MATCHES" as const, value: query.trim() }] : undefined,
  };

  const { data } = usePageData<NonNullable<HadesTextsQuery["hadesQueries"]["texts"]>>(
    "hadesSearch",
    "blog/hadesSearch",
    { pagination: JSON.stringify(pagination) } as unknown as Record<string, string>,
  );
  const items = data?.items ?? [];
  const pageInfo = data?.pageInfo;

  const handleAttach = (textId: string) => {
    startTransition(async () => {
      await attachRemoteObject(postId, `hades:text:${textId}`);
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
              <SearchBar value={searchInput} onChange={setSearchInput} onSearch={handleSearch} placeholder={t("attach.placeholder")} />
            </div>
            <div className={styles.list_body}>
              {items.length === 0 ? (
                <p className={styles.no_items}>{t("attach.empty")}</p>
              ) : (
                items.map((item) => {
                  if (item == null) {
                    return null;
                  }
                  return (
                    <div key={item.id} className={styles.row}>
                      <span className={styles.row_title}>{item.title}</span>
                      <Button
                        variant="secondary"
                        className={styles.attach_button}
                        title={t("attach.attach-label")}
                        aria-label={t("attach.attach-label")}
                        disabled={isPending}
                        onClick={() => handleAttach(item.id)}
                      >
                        <PlusIcon width={16} height={16} />
                      </Button>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </CardBody>
      </Card>
      {pageInfo && pageInfo.totalPages > 1 ? (
        <Pagination
          className={styles.pagination}
          page={page + 1}
          totalPages={pageInfo.totalPages}
          onPageChange={handlePageChange}
        />
      ) : null}
    </>
  );
};

export default AttachTextDialog;
