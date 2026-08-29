import { Skeleton } from "@sun/components";
import styles from "./attach-text-picker-items-skeleton.module.css";

/**
 * Skeleton for the picker items list.
 */
const AttachTextPickerItemsSkeleton = () => {
  return (
    <div className={styles.list_body}>
      <Skeleton className={styles.row} />
      <Skeleton className={styles.row} />
      <Skeleton className={styles.row} />
    </div>
  );
};

export default AttachTextPickerItemsSkeleton;
