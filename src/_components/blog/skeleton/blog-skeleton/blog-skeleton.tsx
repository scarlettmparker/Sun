import Skeleton from "~/components/skeleton";
import styles from "./blog-skeleton.module.css";

const BlogSkeleton = () => {
  return (
    <>
      {Array.from({ length: 5 }, (_, i) => (
        <Skeleton key={i} className={styles.blog_skeleton} />
      ))}
    </>
  );
};

export default BlogSkeleton;
