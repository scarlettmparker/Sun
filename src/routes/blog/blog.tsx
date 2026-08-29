import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { BreadcrumbProvider } from "@sun/components";
import BlogTypeSidebar from "~/components/blog/blog-type-sidebar";
import styles from "./blog.module.css";

/**
 * Blog layout with sidebar and outlet.
 */
const BlogLayout = () => {
  return (
    <BreadcrumbProvider>
      <div className={styles.blog_layout}>
        <Suspense fallback={null}>
          <BlogTypeSidebar />
        </Suspense>
        <div className={styles.blog_main}>
          <Outlet />
        </div>
      </div>
    </BreadcrumbProvider>
  );
};

export default BlogLayout;
