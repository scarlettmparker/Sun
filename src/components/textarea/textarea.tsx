import { cn } from "~/utils/cn";
import styles from "./textarea.module.css";

type TextAreaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement>;

/**
 * Scarlet UI TextArea.
 */
const TextArea = (props: TextAreaProps) => {
  const { className, ...rest } = props;

  return <textarea className={cn(className, styles.textarea)} {...rest} />;
};

export default TextArea;
