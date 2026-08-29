import { Skeleton } from "@sun/components";
import styles from "./ingest-blog-form-fields-skeleton.module.css";

/**
 * Skeleton for ingest form fields.
 */
const IngestBlogFormFieldsSkeleton = () => {
  return (
    <div className={styles.body}>
      <Skeleton className={styles.field} />
      <Skeleton className={styles.field} />
      <Skeleton className={styles.field} />
      <Skeleton className={styles.block} />
    </div>
  );
};

export default IngestBlogFormFieldsSkeleton;
