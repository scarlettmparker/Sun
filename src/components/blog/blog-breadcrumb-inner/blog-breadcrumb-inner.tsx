import { Link, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { BreadcrumbItem } from "@sun/components";
import type { BlogPost } from "~/generated/graphql";
import styles from "./blog-breadcrumb-inner.module.css";

type BlogBreadcrumbInnerProps = {
  /**
   * Blog post for breadcrumb.
   */
  post: BlogPost | null | undefined;
};

/**
 * Inner breadcrumb that renders blog trail.
 */
const BlogBreadcrumbInner = (props: BlogBreadcrumbInnerProps) => {
  const { post } = props;
  const { t } = useTranslation("blog");
  const [searchParams] = useSearchParams();

  if (!post) {
    return null;
  }

  const paramType = searchParams.get("type");
  const typeName = post?.type?.name ?? null;
  const effectiveType = paramType ?? typeName;
  const blogHref = effectiveType ? `/blog?type=${encodeURIComponent(effectiveType)}` : "/blog";

  return (
    <nav aria-label="breadcrumb">
      <ol className={styles.breadcrumb}>
        <BreadcrumbItem>
          <Link to={blogHref}>{t("breadcrumb.blog")}</Link>
        </BreadcrumbItem>
        <BreadcrumbItem active>{post.title}</BreadcrumbItem>
      </ol>
    </nav>
  );
};

export default BlogBreadcrumbInner;
