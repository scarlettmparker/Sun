/**
 * @fileoverview Tests for Separator component.
 * Ensures the shared separator renders and accepts custom attributes.
 */

import { render, screen } from "@testing-library/react";
import { Separator } from "@sun/components";

describe("Separator", () => {
  it("renders a horizontal separator", () => {
    render(<Separator />);
    const separator = screen.getByRole("separator");
    expect(separator).toBeInTheDocument();
    expect(separator.tagName.toLowerCase()).toBe("hr");
  });

  it("accepts a custom className", () => {
    render(<Separator className="custom-separator" />);
    const separator = screen.getByRole("separator");
    expect(separator).toHaveClass("separator", "custom-separator");
  });

  it("passes through additional attributes", () => {
    render(<Separator data-testid="separator" aria-label="divider" />);
    const separator = screen.getByTestId("separator");
    expect(separator).toHaveAttribute("aria-label", "divider");
  });
});
