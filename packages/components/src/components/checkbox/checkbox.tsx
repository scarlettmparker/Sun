import { cn } from "~/utils/cn";
import styles from "./checkbox.module.css";

type CheckboxProps = {
  /**
   * Optional label rendered beside the checkbox.
   */
  label?: React.ReactNode;
} & React.InputHTMLAttributes<HTMLInputElement>;

/**
 * Scarlet UI Checkbox.
 */
const Checkbox = ({ label, className, ...rest }: CheckboxProps) => {
  return (
    <label className={cn(styles.checkbox_wrapper, className)}>
      <input type="checkbox" className={styles.checkbox} {...rest} />
      {label != null && <span className={styles.checkbox_label}>{label}</span>}
    </label>
  );
};

export default Checkbox;