import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { BreadcrumbItem } from "@sun/components";
import { usePageData } from "@sun/ssr/react";
import type { LocateBlogPostQuery } from "~/generated/graphql";
import styles from "./parent-breadcrumb.module.css";

type ParentBreadcrumbProps = {
  /**
   * Parent post id.
   */
  parentId: string;
};

/**
 * Breadcrumb showing new post as child of its parent.
 */
const ParentBreadcrumb = (props: ParentBreadcrumbProps) => {
  const { parentId } = props;
  const { t } = useTranslation("blog");
  const { data: parent } = usePageData<
    LocateBlogPostQuery["blogQueries"]["locateBlogPost"]
  >("blogPost", "blog/:id", { id: parentId });

  const newLabel =
    parent?.title == null ? t("breadcrumb.new") : t("breadcrumb.new-child");

  return (
    <nav aria-label="breadcrumb">
      <ol className={styles.breadcrumb}>
        <BreadcrumbItem>
          <Link to="/blog">{t("breadcrumb.blog")}</Link>
        </BreadcrumbItem>
        {parent?.title == null ? null : (
          <BreadcrumbItem>
            <Link to={`/blog/${parentId}`}>{parent.title}</Link>
          </BreadcrumbItem>
        )}
        <BreadcrumbItem active>{newLabel}</BreadcrumbItem>
      </ol>
    </nav>
  );
};

export default ParentBreadcrumb;
