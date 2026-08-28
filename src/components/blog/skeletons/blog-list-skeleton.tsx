import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./blog-list-skeleton.module.css";

/**
 * Skeleton for blog list month groups.
 */
const BlogListSkeleton = () => {
  return (
    <div className={styles.groups}>
      {Array.from({ length: 3 }, (_, i) => (
        <Card key={i}>
          <CardHeader>
            <Skeleton className={styles.title} />
          </CardHeader>
          <CardBody>
            <Skeleton className={styles.block} />
          </CardBody>
        </Card>
      ))}
    </div>
  );
};

export default BlogListSkeleton;
