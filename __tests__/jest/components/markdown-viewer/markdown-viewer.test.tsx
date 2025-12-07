/**
 * @fileoverview Tests for MarkdownViewer component.
 * Tests the MarkdownViewer component's rendering, highlighting, and prop handling.
 */

import { render, screen } from "@testing-library/react";
import MarkdownViewer from "~/components/markdown-viewer/markdown-viewer";

jest.mock(
  "~/components/textarea/textarea.module.css",
  () => require("~/../testing/jest/mock/css-module-mock").default
);

describe("MarkdownViewer", () => {
  it("renders markdown content with highlighting", () => {
    const markdown = "# Header\n\n**Bold text** and *italic*.";
    render(<MarkdownViewer data-testid="viewer">{markdown}</MarkdownViewer>);

    const viewer = screen.getByTestId("viewer");
    expect(viewer).toHaveClass("markdown_viewer");
    expect(viewer.innerHTML).toContain('<span class="md-h1">Header</span>');
    expect(viewer.innerHTML).toContain(
      '<span class="md-bold">Bold text</span>'
    );
    expect(viewer.innerHTML).toContain('<span class="md-italic">italic</span>');
  });

  it("throws error when children is not a string", () => {
    const consoleSpy = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});
    expect(() => {
      render(<MarkdownViewer>{123}</MarkdownViewer>);
    }).toThrow("MarkdownViewer children must be a string");

    consoleSpy.mockRestore();
  });

  it("handles empty string", () => {
    render(<MarkdownViewer data-testid="viewer">{""}</MarkdownViewer>);
    const viewer = screen.getByTestId("viewer");
    expect(viewer.innerHTML).toBe("");
  });

  it("handles complex markdown", () => {
    const markdown = `# Title

Some text with **bold**, *italic*, and \`code\`.

> Blockquote

- List item 1
- List item 2

[Link](http://example.com)`;

    render(<MarkdownViewer data-testid="viewer">{markdown}</MarkdownViewer>);
    const viewer = screen.getByTestId("viewer");

    expect(viewer.innerHTML).toContain('<span class="md-h1">Title</span>');
    expect(viewer.innerHTML).toContain('<span class="md-bold">bold</span>');
    expect(viewer.innerHTML).toContain('<span class="md-italic">italic</span>');
    expect(viewer.innerHTML).toContain('<span class="md-code">code</span>');
    expect(viewer.innerHTML).toContain(
      '<span class="md-quote">Blockquote</span>'
    );
    expect(viewer.innerHTML).toContain("List item 1");
    expect(viewer.innerHTML).toContain("List item 2");
    expect(viewer.innerHTML).toContain(
      '<a href=\"http://example.com\" target=\"_blank\" class=\"md-link\">Link</a>'
    );
  });

  it("merges custom className with default class", () => {
    render(
      <MarkdownViewer className="custom-class" data-testid="viewer">
        Test
      </MarkdownViewer>
    );
    const viewer = screen.getByTestId("viewer");
    expect(viewer).toHaveClass("markdown_viewer", "custom-class");
  });

  it("passes through other div attributes", () => {
    render(
      <MarkdownViewer
        data-testid="viewer"
        id="markdown-viewer"
        aria-label="Markdown content"
      >
        Test content
      </MarkdownViewer>
    );
    const viewer = screen.getByTestId("viewer");
    expect(viewer).toHaveAttribute("id", "markdown-viewer");
    expect(viewer).toHaveAttribute("aria-label", "Markdown content");
  });

  it("handles special characters and unicode", () => {
    const markdown = "Special chars: éñü 中文\n\n**Жирный** текст";
    render(<MarkdownViewer data-testid="viewer">{markdown}</MarkdownViewer>);
    const viewer = screen.getByTestId("viewer");

    expect(viewer.innerHTML).toContain("Special chars: éñü 中文");
    expect(viewer.innerHTML).toContain('<span class="md-bold">Жирный</span>');
  });

  it("handles long content", () => {
    const markdown = "# ".repeat(100) + "\n\n" + "**bold**".repeat(50);
    render(<MarkdownViewer data-testid="viewer">{markdown}</MarkdownViewer>);
    const viewer = screen.getByTestId("viewer");

    expect(viewer.innerHTML).toContain('<span class="md-h1">');
    expect(viewer.innerHTML).toContain('<span class="md-bold">');
  });

  it("handles nested elements correctly", () => {
    const markdown = "- Item with **bold** and *italic*";
    render(<MarkdownViewer data-testid="viewer">{markdown}</MarkdownViewer>);
    const viewer = screen.getByTestId("viewer");

    expect(viewer.innerHTML).toContain(
      'Item with <span class="md-bold">bold</span> and <span class="md-italic">italic</span>'
    );
  });

  it("does not render script tags", () => {
    const markdown = "<script>alert('xss')</script>";
    render(<MarkdownViewer data-testid="viewer">{markdown}</MarkdownViewer>);
    const viewer = screen.getByTestId("viewer");

    expect(viewer.innerHTML).toContain(
      "&lt;script&gt;alert('xss')&lt;/script&gt;"
    );
    expect(viewer.innerHTML).not.toContain("<script>");
  });

  it("handles headers with numbers and special chars", () => {
    const markdown = "# 1. Header\n## Header-with-dashes";
    render(<MarkdownViewer data-testid="viewer">{markdown}</MarkdownViewer>);
    const viewer = screen.getByTestId("viewer");

    expect(viewer.innerHTML).toContain('<span class="md-h1">1. Header</span>');
    expect(viewer.innerHTML).toContain(
      '<span class="md-h2">Header-with-dashes</span>'
    );
  });

  it("handles multiple consecutive elements", () => {
    const markdown = "**bold****bold2***italic*";
    render(<MarkdownViewer data-testid="viewer">{markdown}</MarkdownViewer>);
    const viewer = screen.getByTestId("viewer");

    expect(viewer.innerHTML).toContain('<span class="md-bold">bold</span>');
    expect(viewer.innerHTML).toContain('<span class="md-bold">bold2</span>');
    expect(viewer.innerHTML).toContain('<span class="md-italic">italic</span>');
  });
});
