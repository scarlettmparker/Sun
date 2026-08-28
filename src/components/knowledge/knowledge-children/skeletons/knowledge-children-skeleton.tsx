import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./knowledge-children-skeleton.module.css";

/**
 * Skeleton for knowledge children.
 */
const KnowledgeChildrenSkeleton = () => {
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

export default KnowledgeChildrenSkeleton;
