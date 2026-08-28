import { Suspense } from "react";
import { useTranslation } from "react-i18next";
import { Sidebar } from "@sun/components";
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
      <h3 className={styles.header}>{t("types.title")}</h3>
      <Suspense fallback={<BlogTypeSidebarSkeleton />}>
        <BlogTypeContainer />
      </Suspense>
    </Sidebar>
  );
};

export default BlogTypeSidebar;
