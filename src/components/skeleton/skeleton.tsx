import { cn } from "~/utils/cn";
import styles from "./skeleton.module.css";

type SkeletonProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Skeleton Component.
 */
const Skeleton = (props: SkeletonProps) => {
  const { className, ...rest } = props;

  return (
    <div
      data-slot="skeleton"
      className={cn(className, styles.skeleton)}
      {...rest}
    />
  );
};

export default Skeleton;
