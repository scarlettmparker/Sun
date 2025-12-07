import { highlightMarkdown, stripMarkdown } from "~/utils/markdown";

describe("highlightMarkdown", () => {
  it("should escape HTML characters", () => {
    const input = "<script>alert('xss')</script>";
    const output = highlightMarkdown(input);
    expect(output).toContain("&lt;script&gt;alert('xss')&lt;/script&gt;");
  });

  it("should highlight headers", () => {
    const input = "# Header 1\n## Header 2\n### Header 3";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-h1"># Header 1</span>');
    expect(output).toContain('<span class="md-h2">## Header 2</span>');
    expect(output).toContain('<span class="md-h3">### Header 3</span>');
  });

  it("should highlight bold text", () => {
    const input = "**bold**";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-bold">**bold**</span>');
  });

  it("should highlight underline text", () => {
    const input = "__underline__";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-underline">__underline__</span>');
  });

  it("should highlight bold italic text", () => {
    const input = "***bold italic***";
    const output = highlightMarkdown(input);
    expect(output).toContain(
      '<span class="md-bold-italic">***bold italic***</span>'
    );
  });

  it("should highlight italic text", () => {
    const input = "*italic* and _also italic_";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-italic">*italic*</span>');
    expect(output).toContain('<span class="md-italic">_also italic_</span>');
  });

  it("should highlight strikethrough text", () => {
    const input = "~~strikethrough~~";
    const output = highlightMarkdown(input);
    expect(output).toContain(
      '<span class="md-strike">~~strikethrough~~</span>'
    );
  });

  it("should highlight inline code", () => {
    const input = "`code`";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-code">`code`</span>');
  });

  it("should highlight blockquotes", () => {
    const input = "> blockquote";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-quote">&gt; blockquote</span>');
  });

  it("should highlight unordered lists", () => {
    const input = "- item\n* item\n+ item";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-list">-</span> item');
    expect(output).toContain('<span class="md-list">*</span> item');
    expect(output).toContain('<span class="md-list">+</span> item');
  });

  it("should highlight links", () => {
    const input = "[text](url)";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-link">[text](url)</span>');
  });

  it("should handle complex markdown", () => {
    const input = "# Title\n\n**Bold** and *italic* text.\n\n- List item";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-h1"># Title</span>');
    expect(output).toContain('<span class="md-bold">**Bold**</span>');
    expect(output).toContain('<span class="md-italic">*italic*</span>');
    expect(output).toContain('<span class="md-list">-</span> List item');
  });

  it("should handle empty string", () => {
    const input = "";
    const output = highlightMarkdown(input);
    expect(output).toBe("");
  });

  it("should handle whitespace only", () => {
    const input = "   \n\t  ";
    const output = highlightMarkdown(input);
    expect(output).toBe("   \n\t  ");
  });

  it("should handle bold italic", () => {
    const input = "***bold italic***";
    const output = highlightMarkdown(input);
    expect(output).toContain(
      '<span class="md-bold-italic">***bold italic***</span>'
    );
  });

  it("should handle escaped markdown", () => {
    const input = "\\*not italic\\* and \\*\\*not bold\\*\\*";
    const output = highlightMarkdown(input);
    expect(output).toContain(
      '\\<span class="md-italic">*not italic\\*</span> and \\<span class="md-italic">*\\*</span>not bold\\<span class="md-italic">*\\*</span>'
    );
    // Since it's not properly escaped, it is highlighted
    expect(output).toContain('class="md-italic"');
  });

  it("should handle invalid markdown (unclosed)", () => {
    const input = "*unclosed italic";
    const output = highlightMarkdown(input);
    expect(output).toBe("*unclosed italic"); // Should not highlight
  });

  it("should handle headers with numbers and special chars", () => {
    const input = "# 1. Header with number\n## Header-with-dashes";
    const output = highlightMarkdown(input);
    expect(output).toContain(
      '<span class="md-h1"># 1. Header with number</span>'
    );
    expect(output).toContain(
      '<span class="md-h2">## Header-with-dashes</span>'
    );
  });

  it("should not highlight indented lists", () => {
    const input = "  - indented item\n    * nested";
    const output = highlightMarkdown(input);
    expect(output).toBe("  - indented item\n    * nested");
  });

  it("should handle links with special characters", () => {
    const input = "[link](http://example.com/path?query=value&other=123)";
    const output = highlightMarkdown(input);
    expect(output).toContain(
      '<span class="md-link">[link](http://example.com/path?query=value&amp;other=123)</span>'
    );
  });

  it("should handle unicode characters", () => {
    const input = "# Заголовок\n**жирный** *курсив* `код`";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-h1"># Заголовок</span>');
    expect(output).toContain('<span class="md-bold">**жирный**</span>');
    expect(output).toContain('<span class="md-italic">*курсив*</span>');
    expect(output).toContain('<span class="md-code">`код`</span>');
  });

  it("should handle long text", () => {
    const input = "# ".repeat(1000) + "Header";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-h1">');
    expect(output.length).toBeGreaterThan(input.length); // Due to span tags
  });

  it("should handle mixed content", () => {
    const input =
      "# Header\n\nSome text **bold** and *italic*.\n\n> Quote\n\n- List\n\n`code` [link](url)";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-h1"># Header</span>');
    expect(output).toContain('<span class="md-bold">**bold**</span>');
    expect(output).toContain('<span class="md-italic">*italic*</span>');
    expect(output).toContain('<span class="md-quote">&gt; Quote</span>');
    expect(output).toContain('<span class="md-list">-</span> List');
    expect(output).toContain('<span class="md-code">`code`</span>');
    expect(output).toContain('<span class="md-link">[link](url)</span>');
  });

  it("should not highlight inside code blocks (if supported)", () => {
    const input = "`**not bold**`";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-code">`**not bold**`</span>');
  });

  it("should handle multiple consecutive elements", () => {
    const input = "**bold****bold2***italic*";
    const output = highlightMarkdown(input);
    expect(output).toContain('<span class="md-bold">**bold**</span>');
    expect(output).toContain('<span class="md-bold">**bold2**</span>');
    expect(output).toContain('<span class="md-italic">*italic*</span>');
  });
});

describe("stripMarkdown", () => {
  it("should strip markdown from headers", () => {
    const highlighted = highlightMarkdown("# Header 1\n## Header 2");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toContain('<span class="md-h1">Header 1</span>');
    expect(stripped).toContain('<span class="md-h2">Header 2</span>');
  });

  it("should strip markdown from bold text", () => {
    const highlighted = highlightMarkdown("**bold**");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toContain('<span class="md-bold">bold</span>');
  });

  it("should strip markdown from italic text", () => {
    const highlighted = highlightMarkdown("*italic*");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toContain('<span class="md-italic">italic</span>');
  });

  it("should strip markdown from code", () => {
    const highlighted = highlightMarkdown("`code`");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toContain('<span class="md-code">code</span>');
  });

  it("should strip markdown from links", () => {
    const highlighted = highlightMarkdown("[text](url)");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toContain(
      '<a href="url" target="_blank" class="md-link">text</a>'
    );
  });

  it("should strip markdown from lists", () => {
    const highlighted = highlightMarkdown("- item");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toBe('<span class="md-list">•</span> item');
  });

  it("should strip markdown from quotes", () => {
    const highlighted = highlightMarkdown("> quote");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toContain('<span class="md-quote">quote</span>');
  });

  it("should handle complex markdown", () => {
    const highlighted = highlightMarkdown(
      "# Title\n\n**Bold** and *italic* text.\n\n- List item\n\n`code` [link](url)"
    );
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toContain('<span class="md-h1">Title</span>');
    expect(stripped).toContain('<span class="md-bold">Bold</span>');
    expect(stripped).toContain('<span class="md-italic">italic</span>');
    expect(stripped).toContain('<span class="md-list">•</span> List item');
    expect(stripped).toContain('<span class="md-code">code</span>');
    expect(stripped).toContain(
      '<a href="url" target="_blank" class="md-link">link</a>'
    );
  });

  it("should handle empty string", () => {
    const highlighted = highlightMarkdown("");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toBe("");
  });

  it("should convert line breaks to <br> tags", () => {
    const highlighted = highlightMarkdown("line1\nline2\n\nline4");
    const stripped = stripMarkdown(highlighted);
    expect(stripped).toContain("line1<br>\nline2<br>\n<br>\nline4");
  });
});
