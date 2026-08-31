import { useTransition } from "react";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Badge, Button } from "@sun/components";
import { PlusIcon } from "@heroicons/react/24/outline";
import { attachRemoteObject } from "~/server/actions/blog-post";
import { CEFR_TO_KEY } from "~/utils/cefr";
import type { HadesTextsQuery } from "~/generated/graphql";
import styles from "./attach-text-picker-items.module.css";

type AttachTextPickerItemsProps = {
  /**
   * Post id to attach to.
   */
  postId: string;
  /**
   * Search query.
   */
  query: string;
  /**
   * Zero-based page index.
   */
  page: number;
};

/**
 * Renders the filtered list of hades texts for the picker.
 */
const AttachTextPickerItems = (props: AttachTextPickerItemsProps) => {
  const { postId, query, page } = props;
  const { t } = useTranslation("blog");
  const [isPending, startTransition] = useTransition();

  const pagination = {
    page,
    size: 10,
    filters: query.trim()
      ? [{ field: "title", operator: "MATCHES" as const, value: query.trim() }]
      : undefined,
  };

  const { data } = usePageData<NonNullable<HadesTextsQuery["hadesQueries"]["texts"]>>(
    "hadesSearch",
    "blog/hadesSearch",
    {
      pagination: JSON.stringify(pagination),
    } as unknown as Record<string, string>,
  );
  const items = data?.items ?? [];
  const { data: levelColours } = usePageData<Record<string, string> | null>(
    "levelColours",
    "levelColours",
  );

  const handleAttach = (textId: string) => {
    startTransition(async () => {
      await attachRemoteObject(postId, `hades:text:${textId}`);
    });
  };

  if (items.length === 0) {
    return <p className={styles.no_items}>{t("attach.empty")}</p>;
  }

  return (
    <div className={styles.list_body}>
      {items.map((item) => {
        if (item == null) {
          return null;
        }
        const colour = levelColours?.[CEFR_TO_KEY[item.level]];
        return (
          <div key={item.id} className={styles.row}>
            <Badge
              className={styles.item_level}
              style={colour ? { backgroundColor: colour } : undefined}
            >
              {item.level}
            </Badge>
            <a
              href={`https://reader.int.scarlettparker.co.uk/texts/${item.id}`}
              target="_blank"
              rel="noreferrer"
              className={styles.row_title_link}
            >
              <span className={styles.row_title}>{item.title}</span>
            </a>
            <Button
              variant="secondary"
              size="icon"
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
      })}
    </div>
  );
};

export default AttachTextPickerItems;
