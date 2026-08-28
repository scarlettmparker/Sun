import { Skeleton } from "@sun/components";
import styles from "./blog-type-sidebar-skeleton.module.css";

/**
 * Skeleton for blog type sidebar.
 */
const BlogTypeSidebarSkeleton = () => {
  return (
    <div className={styles.skeleton_wrapper}>
      {Array.from({ length: 5 }, (_, i) => (
        <Skeleton key={i} className={styles.skeleton_button} />
      ))}
    </div>
  );
};

export default BlogTypeSidebarSkeleton;
