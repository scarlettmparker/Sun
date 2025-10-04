type LabelProps = React.LabelHTMLAttributes<HTMLLabelElement>;

/**
 * Scarlet UI Button.
 */
const Button = (props: LabelProps) => {
  const { ...rest } = props;

  return <label {...rest}>{rest.children}</label>;
};

export default Button;
