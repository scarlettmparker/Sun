import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Skeleton,
} from "@sun/components";
import styles from "./attach-text-picker-skeleton.module.css";

/**
 * Skeleton for attach text picker.
 */
const AttachTextPickerSkeleton = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <Skeleton className={styles.title} />
        </CardTitle>
      </CardHeader>
      <CardBody>
        <div className={styles.body}>
          <Skeleton className={styles.search} />
          <Skeleton className={styles.block} />
        </div>
      </CardBody>
    </Card>
  );
};

export default AttachTextPickerSkeleton;
