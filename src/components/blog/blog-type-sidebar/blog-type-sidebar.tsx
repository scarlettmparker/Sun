import { Suspense } from "react";
import { useTranslation } from "react-i18next";
import { CardTitle, Sidebar } from "@sun/components";
import BlogTypeContainer from "~/components/blog/blog-type-container";
import { BlogTypeSidebarSkeleton } from "./skeletons";
import styles from "./blog-type-sidebar.module.css";

/**
 * Sidebar listing blog post types as filters.
 */
const BlogTypeSidebar = () => {
  const { t } = useTranslation("blog");

  return (
    <Sidebar className={styles.sidebar}>
      {/* TODO: might need a "sidebar title" kind of component */}
      <CardTitle>{t("types.title")}</CardTitle>
      <Suspense fallback={<BlogTypeSidebarSkeleton />}>
        <BlogTypeContainer />
      </Suspense>
    </Sidebar>
  );
};

export default BlogTypeSidebar;
