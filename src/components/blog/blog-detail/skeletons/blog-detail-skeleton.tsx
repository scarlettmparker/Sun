import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./blog-detail-skeleton.module.css";

/**
 * Skeleton for blog detail.
 */
const BlogDetailSkeleton = () => {
  return (
    <div className={styles.detail_wrapper}>
      <Skeleton className={styles.breadcrumb} />
      <Card>
        <CardHeader>
          <Skeleton className={styles.title} />
        </CardHeader>
        <CardBody>
          <Skeleton className={styles.block} />
        </CardBody>
      </Card>
    </div>
  );
};

export default BlogDetailSkeleton;
