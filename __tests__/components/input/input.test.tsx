/**
 * @brief
 * Tests for Input component.
 * Tests the Input component's types, styling, and attribute passing.
 */

import { render, screen } from "@testing-library/react";
import Input from "~/components/input/input";

describe("Input", () => {
  it("renders default text input with correct attributes", () => {
    render(<Input placeholder="Enter text" />);
    const input = screen.getByPlaceholderText("Enter text");
    expect(input).toHaveAttribute("type", "text");
    expect(input).toHaveAttribute("placeholder", "Enter text");
  });

  it("renders range input with horizontal orientation and applies correct classes", () => {
    render(<Input type="range" min="0" max="100" />);
    const input = screen.getByRole("slider");
    expect(input).toHaveAttribute("type", "range");
    expect(input).toHaveClass("range", "horizontal");
  });

  it("renders range input with vertical orientation and applies correct classes", () => {
    render(<Input type="range" orient="vertical" min="0" max="100" />);
    const input = screen.getByRole("slider");
    expect(input).toHaveAttribute("type", "range");
    expect(input).toHaveClass("range", "vertical");
  });

  it("renders checkbox input and applies correct classes", () => {
    render(<Input type="checkbox" />);
    const input = screen.getByRole("checkbox");
    expect(input).toHaveAttribute("type", "checkbox");
    expect(input).toHaveClass("checkbox");
  });

  it("passes through input attributes correctly", () => {
    render(
      <Input
        type="email"
        required
        aria-label="Email input"
        data-testid="email-input"
      />
    );
    const input = screen.getByTestId("email-input");
    expect(input).toHaveAttribute("type", "email");
    expect(input).toHaveAttribute("required");
    expect(input).toHaveAttribute("aria-label", "Email input");
  });

  it("merges custom className with default classes for range", () => {
    render(<Input type="range" className="custom-class" />);
    const input = screen.getByRole("slider");
    expect(input).toHaveClass("range", "horizontal", "custom-class");
  });

  it("merges custom className with default classes for checkbox", () => {
    render(<Input type="checkbox" className="custom-class" />);
    const input = screen.getByRole("checkbox");
    expect(input).toHaveClass("checkbox", "custom-class");
  });

  it("renders with default type text when type is undefined", () => {
    render(<Input />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveAttribute("type", "text");
  });

  it("handles multiple props correctly for range", () => {
    const props = {
      type: "range" as const,
      orient: "vertical" as const,
      min: "0",
      max: "10",
      className: "extra-class",
      "data-testid": "range-input",
    };
    render(<Input {...props} />);
    const input = screen.getByTestId("range-input");
    expect(input).toHaveClass("range", "vertical", "extra-class");
    expect(input).toHaveAttribute("min", "0");
    expect(input).toHaveAttribute("max", "10");
  });
});
