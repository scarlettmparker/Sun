import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Skeleton,
} from "@sun/components";
import styles from "../attached-texts.module.css";

/**
 * Skeleton for attached texts list.
 */
const AttachedTextsSkeleton = () => {
  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <Skeleton style={{ width: "120px", height: "16px" }} />
        </CardTitle>
      </CardHeader>
      <CardBody>
        <div className={styles.list_body}>
          <Skeleton style={{ width: "80%", height: "36px" }} />
          <Skeleton style={{ width: "60%", height: "36px" }} />
        </div>
      </CardBody>
    </Card>
  );
};

export default AttachedTextsSkeleton;
