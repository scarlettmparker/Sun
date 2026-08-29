import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./blog-detail-skeleton.module.css";

/**
 * Skeleton for blog detail.
 */
const BlogDetailSkeleton = () => {
  return (
    <div className={styles.detail_wrapper}>
      <Skeleton className={styles.breadcrumb} />
      <div className={styles.columns}>
        <div className={styles.left_column}>
          <Card>
            <CardHeader>
              <Skeleton className={styles.title} />
            </CardHeader>
            <CardBody>
              <Skeleton className={styles.block} />
            </CardBody>
          </Card>
          <Card>
            <CardHeader>
              <Skeleton className={styles.small_title} />
            </CardHeader>
            <CardBody>
              <Skeleton className={styles.small_block} />
            </CardBody>
          </Card>
          <Card>
            <CardHeader>
              <Skeleton className={styles.small_title} />
            </CardHeader>
            <CardBody>
              <Skeleton className={styles.small_block} />
            </CardBody>
          </Card>
          <Card>
            <CardHeader>
              <Skeleton className={styles.small_title} />
            </CardHeader>
            <CardBody>
              <Skeleton className={styles.small_block} />
            </CardBody>
          </Card>
          <Card>
            <CardHeader>
              <Skeleton className={styles.small_title} />
            </CardHeader>
            <CardBody>
              <Skeleton className={styles.small_block} />
            </CardBody>
          </Card>
        </div>
        <div className={styles.right_column}>
          <Card>
            <CardHeader>
              <Skeleton className={styles.small_title} />
            </CardHeader>
            <CardBody>
              <Skeleton className={styles.small_block} />
            </CardBody>
          </Card>
          <Card>
            <CardHeader>
              <Skeleton className={styles.small_title} />
            </CardHeader>
            <CardBody>
              <Skeleton className={styles.small_block} />
            </CardBody>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default BlogDetailSkeleton;
