import { Outlet } from "react-router-dom";
import { BreadcrumbProvider } from "@sun/components";
import { cn } from "~/utils/cn";
import detailStyles from "~/components/blog/blog-detail/blog-detail.module.css";
import { useNavPortal } from "~/components/layout/menu/top-nav-bar/nav-portal-context";
import styles from "./blog.module.css";

/**
 * Blog layout with outlet.
 */
const BlogLayout = () => {
  const { setOuterSlot } = useNavPortal();

  return (
    <BreadcrumbProvider>
      <div className={styles.blog_layout}>
        <div
          id="blog-detail-wrapper"
          ref={(el) => el?.setAttribute("data-ref", "blog-detail-wrapper")}
          className={cn(styles.blog_main, detailStyles.detail_wrapper)}
        >
          <div
            id="blog-detail-nav-slot"
            ref={(el) => {
              setOuterSlot(el);
              if (el) el.setAttribute("data-ref", "blog-detail-nav-slot");
            }}
            className={detailStyles.nav_slot}
          />
          <Outlet />
        </div>
      </div>
    </BreadcrumbProvider>
  );
};

export default BlogLayout;
