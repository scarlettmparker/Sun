type ButtonProps = {
  /**
   * Button variant.
   */
  variant?: "default" | "secondary";

  /**
   * Button size, icon means square here.
   */
  size?: "default" | "icon";
} & React.ButtonHTMLAttributes<HTMLButtonElement>;

/**
 * Scarlet UI Button.
 */
const Button = (props: ButtonProps) => {
  const { ...rest } = props;

  return <button {...rest}>{rest.children}</button>;
};

export default Button;
