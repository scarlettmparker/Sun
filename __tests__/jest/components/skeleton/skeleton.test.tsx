/**
 * @fileoverview Tests for Skeleton component.
 * Tests the Skeleton component's rendering and prop handling.
 */

import { render, screen } from "@testing-library/react";
import Skeleton from "~/components/skeleton/skeleton";

jest.mock("~/components/skeleton/skeleton.module.css", () => ({
  skeleton: "skeleton",
}));

describe("Skeleton", () => {
  it("renders a div with skeleton class and data-slot", () => {
    render(<Skeleton data-testid="skeleton" />);
    const skeleton = screen.getByTestId("skeleton");
    expect(skeleton.tagName).toBe("DIV");
    expect(skeleton).toHaveAttribute("data-slot", "skeleton");
    expect(skeleton).toHaveClass("skeleton");
  });

  it("merges custom className with default classes", () => {
    render(<Skeleton className="custom-class" data-testid="skeleton" />);
    const skeleton = screen.getByTestId("skeleton");
    expect(skeleton).toHaveClass("skeleton", "custom-class");
  });

  it("passes through other props correctly", () => {
    render(
      <Skeleton
        id="test-id"
        style={{ width: "100px", height: "20px" }}
        data-testid="skeleton"
        aria-label="Loading skeleton"
      />
    );
    const skeleton = screen.getByTestId("skeleton");
    expect(skeleton).toHaveAttribute("id", "test-id");
    expect(skeleton).toHaveAttribute("aria-label", "Loading skeleton");
    expect(skeleton).toHaveStyle({
      width: "100px",
      height: "20px",
    });
  });

  it("handles multiple custom classes", () => {
    render(<Skeleton className="class1 class2" data-testid="skeleton" />);
    const skeleton = screen.getByTestId("skeleton");
    expect(skeleton).toHaveClass("skeleton", "class1", "class2");
  });

  it("renders with default props when no props provided", () => {
    const { container } = render(<Skeleton />);
    const skeleton = container.querySelector('[data-slot="skeleton"]');
    expect(skeleton).toBeInTheDocument();
    expect(skeleton).toHaveClass("skeleton");
  });
});
