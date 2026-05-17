import { cn } from "~/utils/cn";
import "./textarea.module.css";

type TextAreaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement>;

/**
 * Scarlet UI TextArea.
 */
const TextArea = (props: TextAreaProps) => {
  const { className, ...rest } = props;

  return <textarea className={cn(className, "textarea")} {...rest} />;
};

export default TextArea;
