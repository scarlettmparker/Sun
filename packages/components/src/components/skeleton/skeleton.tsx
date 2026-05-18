import { cn } from "~/utils/cn";
import "./skeleton.module.css";

type SkeletonProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Skeleton Component.
 */
const Skeleton = (props: SkeletonProps) => {
  const { className, ...rest } = props;

  return (
    <div data-slot="skeleton" className={cn(className, "skeleton")} {...rest} />
  );
};

export default Skeleton;
