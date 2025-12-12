import Skeleton from "~/components/skeleton";
import styles from "./blog-skeleton.module.css";

const BlogSkeleton = () => {
  return (
    <div className={styles.blog_wrapper}>
      {Array.from({ length: 5 }, (_, i) => (
        <Skeleton key={i} className={styles.blog_skeleton} />
      ))}
    </div>
  );
};

export default BlogSkeleton;
