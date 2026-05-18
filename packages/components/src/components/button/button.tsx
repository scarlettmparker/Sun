import { cn } from "~/utils/cn";
import "./button.module.css";

type ButtonProps = {
  /**
   * Button variant.
   */
  variant?: "default" | "secondary";
} & React.ButtonHTMLAttributes<HTMLButtonElement>;

/**
 * Scarlet UI Button.
 */
const Button = (props: ButtonProps) => {
  const { variant: variant_, ...rest } = props;
  const variant = variant_ ?? "default";

  return (
    <button {...rest} className={cn("button", variant, rest.className)}>
      {rest.children}
    </button>
  );
};

export default Button;
