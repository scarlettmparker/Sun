import { forwardRef } from "react";
import { cn } from "~/utils/cn";
import Input from "../input";
import styles from "./tagged-input.module.css";

type TaggedInputProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Current input value.
   */
  value: string;
  /**
   * Called when the input value changes.
   */
  onChange: (value: string) => void;
  /**
   * Placeholder for the inner input.
   */
  placeholder?: string;
  /**
   * Called when a key is pressed in the input.
   */
  onKeyDown?: React.KeyboardEventHandler<HTMLInputElement>;
};

/**
 * Container that wraps badges and an input.
 */
const TaggedInput = forwardRef<HTMLInputElement, TaggedInputProps>(
  (props, ref) => {
    const { children, value, onChange, placeholder, onKeyDown, className, ...rest } = props;
    const hasChildren = Array.isArray(children)
      ? children.length > 0
      : children != null && children !== false;

    return (
      <div className={cn(styles.container, className)} {...rest}>
        {children}
        <Input
          ref={ref}
          value={value}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => onChange(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder={hasChildren ? undefined : placeholder}
          className={styles.input}
        />
      </div>
    );
  },
);

TaggedInput.displayName = "TaggedInput";

export default TaggedInput;
