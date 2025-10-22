/**
 * @brief
 * Tests for Label component.
 * Tests the Label component's rendering, attribute passing, and children display.
 */

import { render, screen } from "@testing-library/react";
import Label from "~/components/label/label";

describe("Label", () => {
  it("renders label with text content", () => {
    render(<Label>Label text</Label>);
    const label = screen.getByText("Label text");
    expect(label).toBeInTheDocument();
    expect(label.tagName).toBe("LABEL");
  });

  it("passes through label attributes correctly", () => {
    render(
      <Label htmlFor="input-id" aria-label="Test label" data-testid="label">
        Test
      </Label>
    );
    const label = screen.getByTestId("label");
    expect(label).toHaveAttribute("for", "input-id");
    expect(label).toHaveAttribute("aria-label", "Test label");
  });

  it("renders children correctly", () => {
    render(
      <Label>
        <span>Child element</span>
      </Label>
    );
    expect(screen.getByText("Child element")).toBeInTheDocument();
  });

  it("merges custom className", () => {
    render(<Label className="custom-class">Custom label</Label>);
    const label = screen.getByText("Custom label");
    expect(label).toHaveClass("custom-class");
  });

  it("handles onClick event", () => {
    const handleClick = jest.fn();
    render(<Label onClick={handleClick}>Clickable label</Label>);
    const label = screen.getByText("Clickable label");
    label.click();
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it("renders with multiple props correctly", () => {
    const props = {
      htmlFor: "test-input",
      className: "extra-class",
      "data-testid": "multi-prop-label",
    };
    render(<Label {...props}>Multi prop label</Label>);
    const label = screen.getByTestId("multi-prop-label");
    expect(label).toHaveClass("extra-class");
    expect(label).toHaveAttribute("for", "test-input");
  });
});
