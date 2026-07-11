import React from "react";
import { cn } from "~/utils/cn";
import styles from "./separator.module.css";

type SeparatorProps = React.HTMLAttributes<HTMLHRElement>;

/**
 * Separator renders a styled horizontal divider.
 */
const Separator = (props: SeparatorProps) => {
  const { className, ...rest } = props;

  return <hr {...rest} className={cn(styles.separator, className)} />;
};

export default Separator;
