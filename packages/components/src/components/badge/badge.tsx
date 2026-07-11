import { cn } from "~/utils/cn";
import styles from "./badge.module.css";

type BadgeProps = {
  /**
   * Badge variant.
   */
  variant?: "default" | "secondary";
} & React.HTMLAttributes<HTMLSpanElement>;

/**
 * Scarlet UI Badge.
 */
const Badge = ({ variant = "default", className, ...rest }: BadgeProps) => {
  return (
    <span
      {...rest}
      className={cn(styles.badge, styles[variant], className)}
    />
  );
};

export default Badge;
