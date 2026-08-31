import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Skeleton,
} from "@sun/components";
import styles from "./ingest-blog-form-skeleton.module.css";

/**
 * Skeleton for ingest blog form.
 */
const IngestBlogFormSkeleton = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <Skeleton className={styles.title} />
        </CardTitle>
      </CardHeader>
      <CardBody>
        <div className={styles.body}>
          <Skeleton className={styles.field} />
          <Skeleton className={styles.block} />
        </div>
      </CardBody>
    </Card>
  );
};

export default IngestBlogFormSkeleton;
