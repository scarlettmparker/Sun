import styles from "./input.module.css";

type InputProps = React.InputHTMLAttributes<HTMLInputElement>;

/**
 * Scarlet UI Input.
 */
const Input = (props: InputProps) => {
  const { type, ...rest } = props;

  switch (type) {
    case "range":
      // TODO: don't know what the pattern for this is, check other UI libraries
      return (
        <input
          {...rest}
          type="range"
          className={`${rest.className} ${styles.range}`}
        />
      );
    case "checkbox":
      return (
        <input
          {...rest}
          type="checkbox"
          className={`${rest.className} ${styles.checkbox}`}
        />
      );
    default:
      return <input {...rest} type={type} />;
  }
};

export default Input;
