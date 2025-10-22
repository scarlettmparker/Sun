/**
 * @brief
 * Tests for Button component.
 * Tests the Button component's variants, styling, and attribute passing.
 */

import { render, screen } from "@testing-library/react";
import Button from "~/components/button/button";

describe("Button", () => {
  it("renders with default variant and applies correct classes", () => {
    render(<Button>Click me</Button>);
    const button = screen.getByRole("button", { name: /click me/i });
    expect(button).toHaveClass("button", "default");
    expect(button).toHaveTextContent("Click me");
  });

  it("renders with secondary variant and applies correct classes", () => {
    render(<Button variant="secondary">Secondary</Button>);
    const button = screen.getByRole("button", { name: /secondary/i });
    expect(button).toHaveClass("button", "secondary");
    expect(button).toHaveTextContent("Secondary");
  });

  it("passes through button attributes correctly", () => {
    render(
      <Button type="submit" disabled aria-label="Submit button">
        Submit
      </Button>
    );
    const button = screen.getByRole("button", { name: /submit button/i });
    expect(button).toHaveAttribute("type", "submit");
    expect(button).toHaveAttribute("disabled");
    expect(button).toHaveAttribute("aria-label", "Submit button");
  });

  it("merges custom className with default classes", () => {
    render(<Button className="custom-class">Custom</Button>);
    const button = screen.getByRole("button", { name: /custom/i });
    expect(button).toHaveClass("button", "default", "custom-class");
  });

  it("handles onClick event", () => {
    const handleClick = jest.fn();
    render(<Button onClick={handleClick}>Clickable</Button>);
    const button = screen.getByRole("button", { name: /clickable/i });
    button.click();
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("renders children correctly", () => {
    render(
      <Button>
        <span>Child element</span>
      </Button>
    );
    expect(screen.getByText("Child element")).toBeInTheDocument();
  });

  it("defaults to default variant when variant is undefined", () => {
    render(<Button variant={undefined}>Default</Button>);
    const button = screen.getByRole("button", { name: /default/i });
    expect(button).toHaveClass("button", "default");
  });

  it("applies hover and active states implicitly through CSS classes", () => {
    render(<Button>Hover test</Button>);
    const button = screen.getByRole("button", { name: /hover test/i });
    // pseudo-classes like :hover cannot be tested directly in jsdom but we can verify the base classes are applied
    expect(button).toHaveClass("button");
  });

  it("handles multiple props correctly", () => {
    const props = {
      variant: "secondary" as const,
      type: "button" as const,
      className: "extra-class",
      "data-testid": "test-button",
    };
    render(<Button {...props}>Multi prop</Button>);
    const button = screen.getByTestId("test-button");
    expect(button).toHaveClass("button", "secondary", "extra-class");
    expect(button).toHaveAttribute("type", "button");
  });
});
