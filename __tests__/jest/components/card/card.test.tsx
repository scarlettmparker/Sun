/**
 * @fileoverview Tests for Card component.
 * Tests the Card component and its sub-components for rendering, styling, and attribute passing.
 */

import { render, screen } from "@testing-library/react";
import Card, {
  CardHeader,
  CardTitle,
  CardDescription,
  CardBody,
  CardFooter,
} from "~/components/card/card";

describe("Card", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<Card>Card content</Card>);
    const card = screen.getByText("Card content");
    expect(card.tagName).toBe("ARTICLE");
    expect(card).toHaveClass("card");
  });

  it("passes through attributes correctly", () => {
    render(
      <Card data-testid="test-card" aria-label="Test card">
        Content
      </Card>
    );
    const card = screen.getByTestId("test-card");
    expect(card).toHaveAttribute("data-testid", "test-card");
    expect(card).toHaveAttribute("aria-label", "Test card");
  });

  it("merges custom className with default classes", () => {
    render(<Card className="custom-class">Content</Card>);
    const card = screen.getByText("Content");
    expect(card).toHaveClass("card", "custom-class");
  });

  it("renders children correctly", () => {
    render(
      <Card>
        <span>Child element</span>
      </Card>
    );
    expect(screen.getByText("Child element")).toBeInTheDocument();
  });
});

describe("CardHeader", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<CardHeader>Header content</CardHeader>);
    const header = screen.getByText("Header content");
    expect(header.tagName).toBe("HEADER");
    expect(header).toHaveClass("card_header");
  });

  it("passes through attributes correctly", () => {
    render(<CardHeader data-testid="test-header">Content</CardHeader>);
    const header = screen.getByTestId("test-header");
    expect(header).toHaveAttribute("data-testid", "test-header");
  });

  it("merges custom className with default classes", () => {
    render(<CardHeader className="custom-class">Content</CardHeader>);
    const header = screen.getByText("Content");
    expect(header).toHaveClass("card_header", "custom-class");
  });
});

describe("CardTitle", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<CardTitle>Title content</CardTitle>);
    const title = screen.getByText("Title content");
    expect(title.tagName).toBe("H3");
    expect(title).toHaveClass("card_title");
  });

  it("passes through attributes correctly", () => {
    render(<CardTitle data-testid="test-title">Content</CardTitle>);
    const title = screen.getByTestId("test-title");
    expect(title).toHaveAttribute("data-testid", "test-title");
  });

  it("merges custom className with default classes", () => {
    render(<CardTitle className="custom-class">Content</CardTitle>);
    const title = screen.getByText("Content");
    expect(title).toHaveClass("card_title", "custom-class");
  });
});

describe("CardDescription", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<CardDescription>Description content</CardDescription>);
    const description = screen.getByText("Description content");
    expect(description.tagName).toBe("P");
    expect(description).toHaveClass("card_description");
  });

  it("passes through attributes correctly", () => {
    render(
      <CardDescription data-testid="test-description">Content</CardDescription>
    );
    const description = screen.getByTestId("test-description");
    expect(description).toHaveAttribute("data-testid", "test-description");
  });

  it("merges custom className with default classes", () => {
    render(<CardDescription className="custom-class">Content</CardDescription>);
    const description = screen.getByText("Content");
    expect(description).toHaveClass("card_description", "custom-class");
  });
});

describe("CardBody", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<CardBody>Body content</CardBody>);
    const body = screen.getByText("Body content");
    expect(body.tagName).toBe("DIV");
    expect(body).toHaveClass("card_body");
  });

  it("passes through attributes correctly", () => {
    render(<CardBody data-testid="test-body">Content</CardBody>);
    const body = screen.getByTestId("test-body");
    expect(body).toHaveAttribute("data-testid", "test-body");
  });

  it("merges custom className with default classes", () => {
    render(<CardBody className="custom-class">Content</CardBody>);
    const body = screen.getByText("Content");
    expect(body).toHaveClass("card_body", "custom-class");
  });
});

describe("CardFooter", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<CardFooter>Footer content</CardFooter>);
    const footer = screen.getByText("Footer content");
    expect(footer.tagName).toBe("FOOTER");
    expect(footer).toHaveClass("card_footer");
  });

  it("passes through attributes correctly", () => {
    render(<CardFooter data-testid="test-footer">Content</CardFooter>);
    const footer = screen.getByTestId("test-footer");
    expect(footer).toHaveAttribute("data-testid", "test-footer");
  });

  it("merges custom className with default classes", () => {
    render(<CardFooter className="custom-class">Content</CardFooter>);
    const footer = screen.getByText("Content");
    expect(footer).toHaveClass("card_footer", "custom-class");
  });
});
