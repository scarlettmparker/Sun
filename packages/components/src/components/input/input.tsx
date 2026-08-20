import React from "react";
import { cn } from "~/utils/cn";
import styles from "./input.module.css";

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
const Input = React.forwardRef<HTMLInputElement, InputProps>((props, ref) => {
  const { type = "text", ...rest } = props;

  switch (type) {
    case "range": {
      const {
        orient = "horizontal",
        className,
        ...rangeProps
      } = rest as RangeInputProps;
      return (
        <input
          ref={ref}
          {...rangeProps}
          type="range"
          className={cn(className, styles.range, styles[orient])}
        />
      );
    }
    case "checkbox": {
      const { className, ...checkboxProps } = rest;
      return (
        <input
          ref={ref}
          {...checkboxProps}
          type="checkbox"
          className={cn(className, styles.checkbox)}
        />
      );
    }
    case "text": {
      const { className, ...textProps } = rest;
      return (
        <input
          ref={ref}
          {...textProps}
          type="text"
          className={cn(className, styles.text)}
        />
      );
    }
    default: {
      const { className, ...restProps } = rest;
      return (
        <input
          ref={ref}
          {...restProps}
          type={type}
          className={cn(className, styles.text)}
        />
      );
    }
  }
});

Input.displayName = "Input";

export default Input;
