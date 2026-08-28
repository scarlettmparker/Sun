import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Card, CardBody, CardHeader, CardTitle } from "@sun/components";
import type { ChildrenQuery } from "~/generated/graphql";
import styles from "./knowledge-children.module.css";

type KnowledgeChildrenProps = {
  /**
   * Parent post id.
   */
  postId: string;
};

/**
 * Renders direct children of a blog post.
 */
const KnowledgeChildren = (props: KnowledgeChildrenProps) => {
  const { postId } = props;
  const { t } = useTranslation("blog");
  const { data: children } = usePageData<ChildrenQuery["blogQueries"]["children"]>(
    "children",
    "blog/:id/children",
    { id: postId },
  );

  if ((children?.items?.length ?? 0) === 0) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("detail.children")}</CardTitle>
      </CardHeader>
      <CardBody>
        <div className={styles.list_body}>
          {(children?.items ?? []).map((child) => (
            <Link key={child.id} to={`/blog/${child.id}`} className={styles.row_link}>
              <span className={styles.row_title}>{child.title}</span>
            </Link>
          ))}
        </div>
      </CardBody>
    </Card>
  );
};

export default KnowledgeChildren;
