import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./login-skeleton.module.css";

/**
 * Skeleton for login page. Mirrors Card with form.
 */
const LoginSkeleton = () => {
  return (
    <div className={styles.wrapper}>
      <Card className={styles.card}>
        <CardHeader>
          <Skeleton className={styles.title} />
          <Skeleton className={styles.subtitle} />
        </CardHeader>
        <CardBody>
          <div className={styles.body}>
            <Skeleton className={styles.field} />
            <Skeleton className={styles.field} />
            <Skeleton className={styles.button} />
          </div>
        </CardBody>
      </Card>
    </div>
  );
};

export default LoginSkeleton;
