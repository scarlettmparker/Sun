import { useCallback } from "react";
import { cn } from "~/utils/cn";
import TextArea from "../textarea/textarea";
import styles from "./json-text-area.module.css";

type JsonTextAreaProps = {
  /**
   * JSON string value.
   */
  value: string;
  /**
   * Called when the text changes.
   */
  onChange: (value: string) => void;
  /**
   * Called with a validation error, or null when valid.
   */
  onError?: (error: string | null) => void;
} & Omit<
  React.TextareaHTMLAttributes<HTMLTextAreaElement>,
  "value" | "onChange"
>;

/**
 * Text area that wraps the base TextArea and pretty-prints JSON on blur.
 */
const JsonTextArea = (props: JsonTextAreaProps) => {
  const { value, onChange, onError, onBlur, className, ...rest } = props;

  const handleChange = useCallback(
    (event: React.ChangeEvent<HTMLTextAreaElement>) => {
      const next = event.target.value;
      onChange(next);
      if (onError) {
        if (!next.trim()) {
          onError(null);
          return;
        }
        try {
          JSON.parse(next);
          onError(null);
        } catch (err) {
          onError(err instanceof Error ? err.message : "Invalid JSON");
        }
      }
    },
    [onChange, onError],
  );

  const handleBlur = useCallback(
    (event: React.FocusEvent<HTMLTextAreaElement>) => {
      onBlur?.(event);
      if (!value.trim()) return;
      try {
        const parsed = JSON.parse(value);
        const formatted = JSON.stringify(parsed, null, 2);
        if (formatted !== value) {
          onChange(formatted);
        }
        onError?.(null);
      } catch {
        return;
      }
    },
    [value, onChange, onBlur, onError],
  );

  return (
    <TextArea
      value={value}
      onChange={handleChange}
      onBlur={handleBlur}
      className={cn(className, styles.text_area)}
      spellCheck={false}
      {...rest}
    />
  );
};

export default JsonTextArea;
