import { cn } from "~/utils/cn";
import "./input.module.css";

type BaseInputProps = React.InputHTMLAttributes<HTMLInputElement>;

type RangeInputProps = BaseInputProps & {
  /**
   * Type (for range inputs).
   */
  type: "range";

  /**
   * Range orientation.
   */
  orient?: "horizontal" | "vertical";
};

type OtherInputProps = BaseInputProps & {
  /**
   * Exclude types already used by other props.
   */
  type?: Exclude<string, "range">;

  /**
   * Only range has orientation.
   */
  orient?: never;
};

type InputProps = RangeInputProps | OtherInputProps;

/**
 * Scarlet UI Input.
 */
const Input = (props: InputProps) => {
  const { type = "text", ...rest } = props;

  switch (type) {
    case "range": {
      const {
        orient = "horizontal",
        className,
        ...rangeProps
      } = rest as RangeInputProps;
      // TODO: don't know what the pattern for this is, check other UI libraries
      return (
        <input
          {...rangeProps}
          type="range"
          className={cn(className, "range", orient)}
        />
      );
    }
    case "checkbox": {
      const { className, ...checkboxProps } = rest;
      return (
        <input
          {...checkboxProps}
          type="checkbox"
          className={cn(className, "checkbox")}
        />
      );
    }
    case "text": {
      const { className, ...textProps } = rest;
      return (
        <input {...textProps} type="text" className={cn(className, "text")} />
      );
    }
    default: {
      const { className, ...restProps } = rest;
      return <input {...restProps} type={type} className={cn(className, "text")} />;
    }
  }
};

export default Input;
