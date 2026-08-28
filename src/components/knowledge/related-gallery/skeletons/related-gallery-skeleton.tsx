import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./related-gallery-skeleton.module.css";

/**
 * Skeleton for related gallery.
 */
const RelatedGallerySkeleton = () => {
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

export default RelatedGallerySkeleton;
