import { Skeleton } from "@sun/components";
import styles from "./hub-skeleton.module.css";

const HubSkeleton = () => (
  <div className={styles.hub_skeleton}>
    <Skeleton className={styles.header} />
    <div className={styles.grid}>
      {Array.from({ length: 5 }, (_, i) => (
        <Skeleton key={i} className={styles.card} />
      ))}
    </div>
  </div>
);

export default HubSkeleton;
