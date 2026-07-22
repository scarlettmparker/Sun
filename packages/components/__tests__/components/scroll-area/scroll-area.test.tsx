import { render, screen } from "@testing-library/react";
import { ScrollArea } from "@sun/components";

describe("ScrollArea", () => {
  it("renders children", () => {
    render(<ScrollArea>Hello</ScrollArea>);
    expect(screen.getByText("Hello")).toBeInTheDocument();
  });

  it("applies max-height style when provided", () => {
    render(<ScrollArea maxHeight="22rem" data-testid="area">content</ScrollArea>);
    const area = screen.getByTestId("area");
    expect(area).toHaveStyle("max-height: 22rem");
  });

  it("does not set max-height when not provided", () => {
    render(<ScrollArea data-testid="area">content</ScrollArea>);
    const area = screen.getByTestId("area");
    expect(area).not.toHaveStyle("max-height: 22rem");
  });

  it("merges custom className", () => {
    render(<ScrollArea className="custom" data-testid="area">content</ScrollArea>);
    expect(screen.getByTestId("area")).toHaveClass("custom");
  });

  it("passes through HTML attributes", () => {
    render(<ScrollArea data-foo="bar" data-testid="area">content</ScrollArea>);
    expect(screen.getByTestId("area")).toHaveAttribute("data-foo", "bar");
  });
});
