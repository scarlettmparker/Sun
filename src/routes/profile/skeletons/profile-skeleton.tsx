import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./profile-skeleton.module.css";

/**
 * Skeleton for profile page.
 */
const ProfileSkeleton = () => {
  return (
    <div className={styles.wrapper}>
      <Card className={styles.card}>
        <CardHeader>
          <Skeleton className={styles.title} />
        </CardHeader>
        <CardBody>
          <Skeleton className={styles.block} />
        </CardBody>
      </Card>
    </div>
  );
};

export default ProfileSkeleton;
