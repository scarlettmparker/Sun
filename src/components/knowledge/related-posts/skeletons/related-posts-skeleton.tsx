import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./related-posts-skeleton.module.css";

/**
 * Skeleton for related posts.
 */
const RelatedPostsSkeleton = () => {
  return (
    <Card>
      <CardHeader>
        <Skeleton className={styles.title} />
      </CardHeader>
      <CardBody>
        <Skeleton className={styles.block} />
      </CardBody>
    </Card>
  );
};

export default RelatedPostsSkeleton;
