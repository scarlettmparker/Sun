/**
 * @fileoverview Tests for MarkdownEditor component.
 * Tests the MarkdownEditor component's rendering, editing, highlighting, and form integration.
 */

import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import MarkdownEditor from "~/components/markdown-editor/markdown-editor";

jest.mock(
  "~/components/textarea/textarea.module.css",
  () => require("~/../testing/jest/mock/css-module-mock").default
);

describe("MarkdownEditor", () => {
  it("renders contentEditable div with correct attributes", () => {
    render(
      <MarkdownEditor placeholder="Enter markdown" data-testid="editor" />
    );
    const editor = screen.getByTestId("editor");
    expect(editor.tagName).toBe("DIV");
    expect(editor).toHaveAttribute("contenteditable", "true");
    expect(editor).toHaveAttribute("data-placeholder", "Enter markdown");
    expect(editor).toHaveAttribute("role", "textbox");
    expect(editor).toHaveAttribute("aria-multiline", "true");
  });

  it("renders hidden textarea for form integration", () => {
    render(<MarkdownEditor name="content" data-testid="editor" />);
    const textarea = screen.getByDisplayValue("");
    expect(textarea.tagName).toBe("TEXTAREA");
    expect(textarea).toHaveAttribute("name", "content");
    expect(textarea).toHaveStyle({ opacity: 0, pointerEvents: "none" });
  });

  it("passes through textarea attributes correctly", () => {
    render(
      <MarkdownEditor
        name="description"
        required
        aria-label="Markdown editor"
        data-testid="editor"
      />
    );
    const textarea = screen.getByDisplayValue("");
    expect(textarea).toHaveAttribute("name", "description");
    expect(textarea).toHaveAttribute("required");
    expect(textarea).toHaveAttribute("aria-label", "Markdown editor");
  });

  it("merges custom className with default classes", () => {
    render(<MarkdownEditor className="custom-class" data-testid="editor" />);
    const editor = screen.getByTestId("editor");
    expect(editor).toHaveClass("editor", "custom-class");
  });

  it("displays initial value and syncs to textarea", () => {
    const initialValue = "# Hello\n\n**Bold text**";
    render(
      <MarkdownEditor
        value={initialValue}
        name="content"
        data-testid="editor"
        aria-label="content-input"
      />
    );
    const editor = screen.getByTestId("editor");
    expect(editor.innerHTML).toContain('<span class="md-h1"># Hello</span>');
    expect(editor.innerHTML).toContain(
      '<span class="md-bold">**Bold text**</span>'
    );

    const textarea = screen.getByLabelText("content-input");
    expect(textarea).toHaveValue(initialValue);
  });

  it("updates value on user input and calls onChange", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();
    const initialValue = "Initial text";
    render(
      <MarkdownEditor
        value={initialValue}
        onChange={onChange}
        name="content"
        data-testid="editor"
      />
    );

    const editor = screen.getByTestId("editor");
    await user.clear(editor);
    await user.type(editor, "New **bold** text");

    await waitFor(() => {
      expect(onChange).toHaveBeenCalled();
      const lastCall = onChange.mock.calls[onChange.mock.calls.length - 1][0];
      expect(lastCall.target.value).toBe("New **bold** text");
      expect(lastCall.target.name).toBe("content");
    });

    const textarea = screen.getByDisplayValue("New **bold** text");
    expect(textarea).toHaveValue("New **bold** text");
  });

  it("handles empty value", () => {
    render(<MarkdownEditor value="" name="content" data-testid="editor" />);
    const editor = screen.getByTestId("editor");
    expect(editor.innerHTML).toBe("");
    const textarea = screen.getByDisplayValue("");
    expect(textarea).toHaveValue("");
  });

  it("handles undefined value", () => {
    render(<MarkdownEditor name="content" data-testid="editor" />);
    const editor = screen.getByTestId("editor");
    expect(editor.innerHTML).toBe("");
    const textarea = screen.getByDisplayValue("");
    expect(textarea).toHaveValue("");
  });

  it("applies highlighting on input", async () => {
    const user = userEvent.setup();
    render(<MarkdownEditor name="content" data-testid="editor" />);

    const editor = screen.getByTestId("editor");
    await user.type(editor, "# Header");

    await waitFor(() => {
      expect(editor.innerHTML).toContain('<span class="md-h1"># Header</span>');
    });
  });

  it("prevents default on Enter without Shift", async () => {
    render(<MarkdownEditor name="content" data-testid="editor" />);

    const editor = screen.getByTestId("editor");

    const event = new KeyboardEvent("keydown", {
      key: "Enter",
      shiftKey: false,
      bubbles: true,
      cancelable: true,
    });

    const preventDefaultSpy = jest.spyOn(event, "preventDefault");

    fireEvent(editor, event);

    expect(preventDefaultSpy).toHaveBeenCalled();
  });

  it("allows Enter with Shift", async () => {
    render(<MarkdownEditor name="content" data-testid="editor" />);

    const editor = screen.getByTestId("editor");
    const mockPreventDefault = jest.fn();
    fireEvent.keyDown(editor, {
      key: "Enter",
      shiftKey: true,
      preventDefault: mockPreventDefault,
    });

    expect(mockPreventDefault).not.toHaveBeenCalled();
  });

  it("syncs external value changes", () => {
    const { rerender } = render(
      <MarkdownEditor value="Old value" name="content" data-testid="editor" />
    );
    const editor = screen.getByTestId("editor");

    expect(editor.textContent).toBe("Old value");

    rerender(
      <MarkdownEditor value="New value" name="content" data-testid="editor" />
    );
    expect(editor.textContent).toBe("New value");

    const textarea = screen.getByDisplayValue("New value");
    expect(textarea).toHaveValue("New value");
  });

  it("handles placeholder display", () => {
    render(
      <MarkdownEditor placeholder="Start typing..." data-testid="editor" />
    );
    const editor = screen.getByTestId("editor");
    expect(editor).toHaveAttribute("data-placeholder", "Start typing...");
  });

  it("handles custom style prop", () => {
    const customStyle = { fontSize: "14px", color: "blue" };
    render(<MarkdownEditor style={customStyle} data-testid="editor" />);
    const editor = screen.getByTestId("editor");
    expect(editor).toHaveStyle({
      fontSize: "14px",
      color: "rgb(0, 0, 255)",
      whiteSpace: "pre-wrap",
    });
  });

  it("maintains cursor position during highlighting", async () => {
    const user = userEvent.setup();
    render(
      <MarkdownEditor value="Some text" name="content" data-testid="editor" />
    );

    const editor = screen.getByTestId("editor");
    // Focus and set cursor at the end
    editor.focus();
    const range = document.createRange();
    range.selectNodeContents(editor);
    range.collapse(false);
    const selection = window.getSelection();
    selection?.removeAllRanges();
    selection?.addRange(range);

    await user.type(editor, " **bold**");

    // Check that the text is updated correctly
    expect(editor.textContent).toBe("Some text **bold**");
  });

  it("handles rapid typing", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();
    render(
      <MarkdownEditor onChange={onChange} name="content" data-testid="editor" />
    );

    const editor = screen.getByTestId("editor");
    await user.type(editor, "Quick typing test");

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledTimes("Quick typing test".length);
    });

    const textarea = screen.getByDisplayValue("Quick typing test");
    expect(textarea).toHaveValue("Quick typing test");
  });

  it("handles special characters and unicode", async () => {
    const user = userEvent.setup();
    render(<MarkdownEditor name="content" data-testid="editor" />);

    const editor = screen.getByTestId("editor");
    await user.type(editor, "Special chars: éñü 中文");

    await waitFor(() => {
      expect(editor.textContent).toBe("Special chars: éñü 中文");
    });

    const textarea = screen.getByDisplayValue("Special chars: éñü 中文");
    expect(textarea).toHaveValue("Special chars: éñü 中文");
  });

  it("handles paste events", async () => {
    const onChange = jest.fn();
    render(
      <MarkdownEditor onChange={onChange} name="content" data-testid="editor" />
    );

    const editor = screen.getByTestId("editor");
    editor.focus();
    const pasteData = "Pasted **markdown** text";
    fireEvent.paste(editor, {
      clipboardData: {
        getData: () => pasteData,
      },
    });

    await waitFor(() => {
      const textarea = screen.getByDisplayValue(pasteData);
      expect(textarea).toHaveValue(pasteData);
    });

    const textarea = screen.getByDisplayValue(pasteData);
    expect(textarea).toHaveValue(pasteData);
  });

  it("correctly pastes at cursor position", async () => {
    const onChange = jest.fn();
    const initialValue = "Hello ";
    render(
      <MarkdownEditor
        value={initialValue}
        onChange={onChange}
        name="content"
        data-testid="editor"
      />
    );

    const editor = screen.getByTestId("editor");
    // Set cursor at the end
    editor.focus();
    const range = document.createRange();
    range.selectNodeContents(editor);
    range.collapse(false);
    const selection = window.getSelection();
    selection?.removeAllRanges();
    selection?.addRange(range);

    const pasteData = "world";
    fireEvent.paste(editor, {
      clipboardData: {
        getData: () => pasteData,
      },
    });

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledWith(
        expect.objectContaining({
          target: expect.objectContaining({ value: "Hello world" }),
        })
      );
    });

    const textarea = screen.getByDisplayValue("Hello world");
    expect(textarea).toHaveValue("Hello world");
  });
});
