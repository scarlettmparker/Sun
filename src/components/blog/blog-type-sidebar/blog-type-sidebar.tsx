import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Button, Sidebar } from "@sun/components";
import type { BlogPostTypesQuery, LocateBlogPostQuery } from "~/generated/graphql";
import styles from "./blog-type-sidebar.module.css";

/**
 * Sidebar listing blog post types as filters.
 */
const BlogTypeSidebar = () => {
  const { t } = useTranslation("blog");
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { data: types } = usePageData<BlogPostTypesQuery["blogQueries"]["blogPostTypes"]>(
    "types",
    "home",
    {},
  );
  const { data: post } = usePageData<LocateBlogPostQuery["blogQueries"]["locateBlogPost"]>(
    "blogPost",
    "blog/:id",
    { id: id ?? "" },
  );

  const queryType = searchParams.get("type");
  const selectedType = id ? (post?.type?.name ?? null) : queryType;

  const handleSelect = (type: string | null) => {
    if (id) {
      if (type == null) {
        navigate("/blog");
      } else {
        navigate(`/blog?type=${encodeURIComponent(type)}`);
      }
      return;
    }
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (type == null) {
        next.delete("type");
      } else {
        next.set("type", type);
      }
      next.delete("page");
      return next;
    });
  };

  return (
    <Sidebar className={styles.sidebar}>
      <h3 className={styles.header}>{t("types.title")}</h3>
      <Button variant={selectedType == null ? "default" : "secondary"} onClick={() => handleSelect(null)}>
        {t("filter.all")}
      </Button>
      {(types ?? []).map((postType) => (
        <Button
          key={postType.id}
          variant={selectedType === postType.name ? "default" : "secondary"}
          onClick={() => handleSelect(postType.name)}
        >
          {t(`types.${postType.name}`, { defaultValue: postType.name })}
        </Button>
      ))}
    </Sidebar>
  );
};

export default BlogTypeSidebar;
