/**
 * @fileoverview Tests for Sidebar component.
 * Tests the Sidebar component's styling, children rendering, and attribute passing.
 */

import { render, screen } from "@testing-library/react";
import Sidebar from "~/components/sidebar/sidebar";

describe("Sidebar", () => {
  it("renders with children and applies correct classes", () => {
    render(
      <Sidebar>
        <div>Menu Item 1</div>
        <div>Menu Item 2</div>
      </Sidebar>
    );
    const sidebar = screen.getByText("Menu Item 1").parentElement;
    expect(sidebar).toHaveClass("sidebar");
    expect(screen.getByText("Menu Item 1")).toBeInTheDocument();
    expect(screen.getByText("Menu Item 2")).toBeInTheDocument();
  });

  it("passes through div attributes correctly", () => {
    render(
      <Sidebar id="sidebar-id" aria-label="Navigation sidebar">
        <div>Content</div>
      </Sidebar>
    );
    const sidebar = screen.getByLabelText("Navigation sidebar");
    expect(sidebar).toHaveAttribute("id", "sidebar-id");
    expect(sidebar).toHaveAttribute("aria-label", "Navigation sidebar");
  });

  it("merges custom className with default classes", () => {
    render(
      <Sidebar className="custom-class">
        <div>Content</div>
      </Sidebar>
    );
    const sidebar = screen.getByText("Content").parentElement;
    expect(sidebar).toHaveClass("sidebar", "custom-class");
  });

  it("renders children correctly", () => {
    render(
      <Sidebar>
        <button>Button Item</button>
        <a href="#">Link Item</a>
      </Sidebar>
    );
    expect(
      screen.getByRole("button", { name: /button item/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /link item/i })
    ).toBeInTheDocument();
  });

  it("handles multiple props correctly", () => {
    const props = {
      className: "extra-class",
      "data-testid": "test-sidebar",
      style: { width: "200px" },
    };
    render(
      <Sidebar {...props}>
        <div>Test</div>
      </Sidebar>
    );
    const sidebar = screen.getByTestId("test-sidebar");
    expect(sidebar).toHaveClass("sidebar", "extra-class");
    expect(sidebar).toHaveStyle({ width: "200px" });
  });
});
