import { Card, CardBody, Skeleton } from "@sun/components";
import styles from "./create-blog-skeleton.module.css";

/**
 * Skeleton for the create blog page.
 */
const CreateBlogSkeleton = () => (
  <Card className={styles.card}>
    <CardBody className={styles.skeleton_body}>
      <Skeleton className={styles.skeleton_field} />
      <Skeleton className={styles.skeleton_field} />
      <Skeleton className={styles.skeleton_block} />
    </CardBody>
  </Card>
);

export default CreateBlogSkeleton;
