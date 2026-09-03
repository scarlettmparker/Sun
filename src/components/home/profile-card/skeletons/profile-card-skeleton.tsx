import { CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./profile-card-skeleton.module.css";

/**
 * Skeleton for profile card.
 */
const ProfileCardSkeleton = () => {
  return (
    <>
      <CardHeader>
        <Skeleton className={styles.title} />
      </CardHeader>
      <CardBody>
        <div className={styles.body}>
          <Skeleton className={styles.line} />
          <Skeleton className={styles.line_short} />
        </div>
      </CardBody>
    </>
  );
};

export default ProfileCardSkeleton;
