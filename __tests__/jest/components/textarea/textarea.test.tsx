/**
 * @fileoverview Tests for TextArea component.
 * Tests the TextArea component's rendering, styling, and attribute passing.
 */

import { render, screen } from "@testing-library/react";
import TextArea from "~/components/textarea/textarea";

describe("TextArea", () => {
  it("renders textarea element with correct attributes", () => {
    render(<TextArea placeholder="Enter text" data-testid="textarea" />);
    const textarea = screen.getByTestId("textarea");
    expect(textarea.tagName).toBe("TEXTAREA");
    expect(textarea).toHaveAttribute("placeholder", "Enter text");
  });

  it("passes through textarea attributes correctly", () => {
    render(
      <TextArea
        rows={5}
        cols={30}
        required
        aria-label="Description textarea"
        data-testid="textarea"
      />
    );
    const textarea = screen.getByTestId("textarea");
    expect(textarea).toHaveAttribute("rows", "5");
    expect(textarea).toHaveAttribute("cols", "30");
    expect(textarea).toHaveAttribute("required");
    expect(textarea).toHaveAttribute("aria-label", "Description textarea");
  });

  it("merges custom className with default classes", () => {
    render(<TextArea className="custom-class" data-testid="textarea" />);
    const textarea = screen.getByTestId("textarea");
    expect(textarea).toHaveClass("textarea", "custom-class");
  });

  it("handles multiple props correctly", () => {
    const props = {
      placeholder: "Type here",
      rows: 10,
      className: "extra-class",
      "data-testid": "textarea",
    };
    render(<TextArea {...props} />);
    const textarea = screen.getByTestId("textarea");
    expect(textarea).toHaveAttribute("placeholder", "Type here");
    expect(textarea).toHaveAttribute("rows", "10");
    expect(textarea).toHaveClass("textarea", "extra-class");
  });

  it("renders with default styling", () => {
    render(<TextArea data-testid="textarea" />);
    const textarea = screen.getByTestId("textarea");
    expect(textarea).toHaveClass("textarea");
  });
});
