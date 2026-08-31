import { cn } from "~/utils/cn";
import styles from "./button.module.css";

type ButtonProps = {
  /**
   * Button variant.
   */
  variant?: "default" | "secondary" | "destructive";
  /**
   * Button size.
   */
  size?: "default" | "icon";
} & React.ButtonHTMLAttributes<HTMLButtonElement>;

/**
 * Scarlet UI Button.
 */
const Button = (props: ButtonProps) => {
  const { variant: variant_, size: size_, ...rest } = props;
  const variant = variant_ ?? "default";
  const size = size_ ?? "default";

  return (
    <button
      {...rest}
      className={cn(
        styles.button,
        styles[variant],
        size !== "default" ? styles[size] : undefined,
        rest.className,
      )}
    >
      {rest.children}
    </button>
  );
};

export default Button;
