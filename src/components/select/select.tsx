import React, { useState, useRef, useEffect } from "react";
import { cn } from "~/utils/cn";
import styles from "./select.module.css";

/**
 * Props for the Select component, extending standard select attributes.
 */
type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement> & {
  "data-testid"?: string;
};

/**
 * Scarlet UI Select component.
 */
const Select = (props: SelectProps) => {
  const {
    value,
    onChange,
    children,
    className,
    id,
    style,
    "data-testid": testId,
    defaultValue,
    ...rest
  } = props;
  const [isOpen, setIsOpen] = useState(false);
  const [selectedValue, setSelectedValue] = useState(
    value || defaultValue || ""
  );
  const selectRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setSelectedValue(value || defaultValue || "");
  }, [value, defaultValue]);

  const options = React.Children.toArray(children)
    .filter((child): child is React.ReactElement<SelectOptionProps> => {
      return React.isValidElement(child) && child.type === SelectOption;
    })
    .map((child) => ({
      value: child.props.value,
      label: child.props.children,
    }));

  const selectedOption = options?.find((opt) => opt.value === selectedValue);

  /**
   * Toggles the dropdown open/close state.
   */
  const handleToggle = () => setIsOpen(!isOpen);

  /**
   * Selects an option and closes the dropdown.
   */
  const handleSelect = (optionValue: string) => {
    setSelectedValue(optionValue);
    const event = {
      target: { value: optionValue },
      currentTarget: { value: optionValue },
    } as React.ChangeEvent<HTMLSelectElement>;
    onChange?.(event);
    setIsOpen(false);
  };

  /**
   * Handles keyboard navigation for the select button.
   */
  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handleToggle();
    } else if (event.key === "Escape") {
      setIsOpen(false);
    } else if (event.key === "ArrowDown" && !isOpen) {
      event.preventDefault();
      setIsOpen(true);
    }
  };

  /**
   * Closes the dropdown when clicking outside the component.
   */
  const handleClickOutside = (event: MouseEvent) => {
    if (
      selectRef.current &&
      !selectRef.current.contains(event.target as Node)
    ) {
      setIsOpen(false);
    }
  };

  useEffect(() => {
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  /**
   * Handles button click to toggle dropdown.
   */
  const handleButtonClick = () => handleToggle();

  /**
   * Handles option click to select.
   */
  const handleOptionClick = (optionValue: string) => handleSelect(optionValue);

  /**
   * Handles option key down for selection.
   */
  const handleOptionKeyDown =
    (optionValue: string) => (event: React.KeyboardEvent) => {
      if (event.key === "Enter") {
        handleSelect(optionValue);
      }
    };

  /**
   * Handles change events from the hidden select element.
   */
  const handleHiddenSelectChange = (
    e: React.ChangeEvent<HTMLSelectElement>
  ) => {
    setSelectedValue(e.target.value);
    onChange?.(e);
  };

  return (
    <div
      className={cn(styles.selectContainer, className)}
      id={id}
      style={style}
      data-testid={testId}
      ref={selectRef}
    >
      <select
        {...rest}
        id={id}
        value={selectedValue}
        onChange={handleHiddenSelectChange}
        data-testid="hidden-select"
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          opacity: 0,
          pointerEvents: "none",
        }}
      >
        {children}
      </select>
      <button
        type="button"
        className={styles.select}
        onClick={handleButtonClick}
        onKeyDown={handleKeyDown}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-label="Select option"
      >
        {selectedOption ? selectedOption.label : "Select..."}
      </button>
      {isOpen && (
        <div className={styles.dropdown} role="listbox">
          {options?.map((option) => (
            <div
              key={option.value}
              className={cn(
                styles.option,
                option.value === selectedValue && styles.selected
              )}
              onClick={() => handleOptionClick(option.value)}
              role="option"
              aria-selected={option.value === selectedValue}
              tabIndex={0}
              onKeyDown={handleOptionKeyDown(option.value)}
            >
              {option.label}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

type SelectOptionProps = {
  value: string;
  children: React.ReactNode;
} & Omit<React.OptionHTMLAttributes<HTMLOptionElement>, "value" | "children">;

/**
 * Scarlet UI SelectOption component.
 */
const SelectOption = (props: SelectOptionProps) => {
  const { children, ...rest } = props;

  return <option {...rest}>{children}</option>;
};

export default Select;
export { SelectOption };
