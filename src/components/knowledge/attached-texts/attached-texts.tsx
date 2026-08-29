import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Badge, Card, CardBody, CardHeader, CardTitle } from "@sun/components";
import { CEFR_TO_KEY } from "~/utils/cefr";
import type { LocateReaderTextsQuery } from "~/generated/graphql";
import styles from "./attached-texts.module.css";

type AttachedTextsProps = {
  /**
   * Blog post id to load attachments for.
   */
  postId: string;
};

/**
 * Renders attached reader texts resolved via batch locateReaderTexts.
 */
const AttachedTexts = (props: AttachedTextsProps) => {
  const { postId } = props;
  const { t } = useTranslation("blog");
  const { data: attachedTexts } = usePageData<
    NonNullable<LocateReaderTextsQuery["hadesQueries"]["locateReaderTexts"]>
  >("attachedTexts", "blog/:id/attachedTexts", { id: postId });
  const { data: levelColours } = usePageData<Record<string, string> | null>(
    "levelColours",
    "levelColours",
  );

  if ((attachedTexts?.length ?? 0) === 0) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("detail.attached-texts")}</CardTitle>
      </CardHeader>
      <CardBody>
        <ul className={styles.list_body}>
          {(attachedTexts ?? []).map((item) => {
            if (item == null) {
              return null;
            }
            return (
              <li key={item.id}>
                <div className={styles.row}>
                  <Badge
                    className={styles.item_level}
                    style={
                      levelColours?.[CEFR_TO_KEY[item.level]]
                        ? {
                            backgroundColor:
                              levelColours[CEFR_TO_KEY[item.level]],
                          }
                        : undefined
                    }
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
                </div>
              </li>
            );
          })}
        </ul>
      </CardBody>
    </Card>
  );
};

export default AttachedTexts;
