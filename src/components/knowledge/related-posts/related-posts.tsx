import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Card, CardBody, CardHeader, CardTitle } from "@sun/components";
import type { ListBlogPostsByRemoteObjectsQuery } from "~/generated/graphql";
import styles from "./related-posts.module.css";

type RelatedPostsProps = {
  /**
   * Remote object ids for blog posts.
   */
  ids: string[];
};

/**
 * Renders related blog posts resolved via remoteObject.
 */
const RelatedPosts = (props: RelatedPostsProps) => {
  const { ids } = props;
  const { t } = useTranslation("blog");
  const { data: relatedPosts } = usePageData<
    NonNullable<
      ListBlogPostsByRemoteObjectsQuery["blogQueries"]["listByRemoteObjects"]
    >
  >("relatedPosts", "blog/relatedPosts", {
    ids: JSON.stringify(ids),
  } as unknown as Record<string, string>);

  if (ids.length === 0) {
    return null;
  }

  if ((relatedPosts?.length ?? 0) === 0) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("detail.related")}</CardTitle>
      </CardHeader>
      <CardBody>
        <div className={styles.list_body}>
          {(relatedPosts ?? []).map((post) => {
            if (post == null) {
              return null;
            }
            return (
              <Link
                key={post.id}
                to={`/blog/${post.id}`}
                className={styles.row_link}
              >
                <span className={styles.row_title}>{post.title}</span>
              </Link>
            );
          })}
        </div>
      </CardBody>
    </Card>
  );
};

export default RelatedPosts;
