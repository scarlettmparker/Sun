type InputProps = React.InputHTMLAttributes<HTMLInputElement>;

/**
 * Scarlet UI Input.
 */
const Input = (props: InputProps) => {
  const { ...rest } = props;

  return <input {...rest} />;
};

export default Input;
