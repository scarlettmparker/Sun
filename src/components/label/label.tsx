type LabelProps = React.LabelHTMLAttributes<HTMLLabelElement>;

/**
 * Scarlet UI Label.
 */
const Label = (props: LabelProps) => {
  const { ...rest } = props;

  return <label {...rest}>{rest.children}</label>;
};

export default Label;
