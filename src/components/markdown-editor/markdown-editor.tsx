import React, { useRef, useLayoutEffect, useState } from "react";
import { cn } from "~/utils/cn";
import { highlightMarkdown } from "~/utils/markdown";
import styles from "./markdown-editor.module.css";
import textAreaStyles from "~/components/textarea/textarea.module.css";

type MarkdownEditorProps = React.TextareaHTMLAttributes<HTMLTextAreaElement> & {
  "data-testid"?: string;
};

type HighlightFunction = (
  /**
   * Raw text content to highlight.
   */
  text: string,

  /**
   * HTMLDivElement where the highlighted content will be applied
   */
  el: HTMLDivElement,

  /**
   * Callback function that restores the cursor
   * position after the inner HTML has been updated
   */
  restoreCursor: (el: HTMLElement) => void
) => void;

/**
 * Scarlet UI Markdown Editor.
 */
const MarkdownEditor = (props: MarkdownEditorProps) => {
  const {
    className,
    value,
    onChange,
    placeholder,
    style,
    "data-testid": testId,
    ...rest
  } = props;

  const contentEditableRef = useRef<HTMLDivElement>(null);
  const cursorRef = useRef<number>(0);
  const undoStackRef = useRef<string[]>([]);
  const redoStackRef = useRef<string[]>([]);
  const [currentValue, setCurrentValue] = useState(String(props.value || ""));

  const MAX_HISTORY = 100;

  /**
   * Function to apply syntax highlighting.
   */
  const highlight: HighlightFunction = (text, el, restoreCursor) => {
    if (el && el.textContent === text) {
      saveCursor(el);
      el.innerHTML = highlightMarkdown(text);
      restoreCursor(el);
    }
  };

  /**
   * Saves the current cursor position as a plain text offset.
   */
  const saveCursor = (el: HTMLElement) => {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) return;

    const range = selection.getRangeAt(0);
    const preCaretRange = range.cloneRange();
    preCaretRange.selectNodeContents(el);
    preCaretRange.setEnd(range.endContainer, range.endOffset);
    cursorRef.current = preCaretRange.toString().length;
  };

  /**
   * Restores the cursor position based on the saved text offset.
   * Traverses the DOM tree to find the correct text node and offset.
   */
  const restoreCursor = (el: HTMLElement) => {
    const selection = window.getSelection();
    if (!selection) return;

    let charIndex = cursorRef.current;
    const range = document.createRange();
    range.setStart(el, 0);
    range.collapse(true);

    const stack: Node[] = [el];
    let found = false;

    while (stack.length > 0 && !found) {
      const node = stack.pop()!;
      if (node.nodeType === Node.TEXT_NODE) {
        const nextCharIndex = node.textContent?.length || 0;
        if (charIndex <= nextCharIndex) {
          range.setStart(node, charIndex);
          range.collapse(true);
          found = true;
        } else {
          charIndex -= nextCharIndex;
        }
      } else {
        // Push children in reverse order to traverse depth-first correctly
        for (let i = node.childNodes.length - 1; i >= 0; i--) {
          stack.push(node.childNodes[i]);
        }
      }
    }

    selection.removeAllRanges();
    selection.addRange(range);
  };

  /**
   * Pushes the current text state to the undo stack, maintaining a maximum history size.
   */
  const pushToUndoStack = (text: string) => {
    undoStackRef.current.push(text);
    if (undoStackRef.current.length > MAX_HISTORY) {
      undoStackRef.current.shift();
    }
    redoStackRef.current = [];
  };

  /**
   * Sets the text content and moves the cursor to the end.
   */
  const setTextAndRestoreCursorToEnd = (text: string, el: HTMLDivElement) => {
    el.textContent = text;
    const range = document.createRange();
    range.selectNodeContents(el);
    range.collapse(false);
    const selection = window.getSelection();
    selection?.removeAllRanges();
    selection?.addRange(range);
    highlight(text, el, restoreCursor);
  };

  /**
   * Handles undo operation by restoring the previous text state from the undo stack.
   */
  const handleUndo = () => {
    const el = contentEditableRef.current;

    if (undoStackRef.current.length > 0 && el) {
      const currentText = el.textContent || "";
      redoStackRef.current.push(currentText);
      const previousText = undoStackRef.current.pop()!;
      setCurrentValue(previousText);
      setTextAndRestoreCursorToEnd(previousText, el);
    }
  };

  /**
   * Handles redo operation by restoring the next text state from the redo stack.
   */
  const handleRedo = () => {
    const el = contentEditableRef.current;

    if (redoStackRef.current.length > 0 && el) {
      const currentText = el.textContent || "";
      undoStackRef.current.push(currentText);
      const nextText = redoStackRef.current.pop()!;
      setCurrentValue(nextText);
      setTextAndRestoreCursorToEnd(nextText, el);
    }
  };

  /**
   * Handles key down events, preventing default Enter behavior and handling undo/redo shortcuts.
   */
  const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
    } else if (e.ctrlKey || e.metaKey) {
      if (e.key === "z" && !e.shiftKey) {
        e.preventDefault();
        handleUndo();
      } else if (e.key === "y" || (e.key === "z" && e.shiftKey)) {
        e.preventDefault();
        handleRedo();
      }
    }
  };

  /**
   * Handles input events by updating the value, pushing to undo stack, and triggering re-highlighting.
   */
  const handleInput = () => {
    const el = contentEditableRef.current;
    if (!el) return;

    const rawText = el.textContent || "";
    pushToUndoStack(currentValue);
    setCurrentValue(rawText);

    if (onChange) {
      const event = {
        target: { value: rawText, name: props.name },
        currentTarget: { value: rawText, name: props.name },
      } as React.ChangeEvent<HTMLTextAreaElement>;
      onChange(event);
    }

    saveCursor(el);
    highlight(rawText, el, restoreCursor);
  };

  /**
   * Handles paste events by inserting the pasted text at the cursor position and triggering re-highlighting.
   */
  const handlePaste = (e: React.ClipboardEvent<HTMLDivElement>) => {
    e.preventDefault();
    let pasteText = e.clipboardData.getData("text");
    pasteText = pasteText.replace(/\r/g, "");

    const el = contentEditableRef.current;
    if (!el) return;

    pushToUndoStack(currentValue);

    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      range.deleteContents();
      const textNode = document.createTextNode(pasteText);
      range.insertNode(textNode);
      range.setStartAfter(textNode);
      range.setEndAfter(textNode);
      selection.removeAllRanges();
      selection.addRange(range);
    } else {
      el.textContent += pasteText;
    }

    const newText = el.textContent || "";
    setCurrentValue(newText);

    if (onChange) {
      const event = {
        target: { value: newText, name: props.name },
        currentTarget: { value: newText, name: props.name },
      } as React.ChangeEvent<HTMLTextAreaElement>;
      onChange(event);
    }

    saveCursor(el);
    highlight(newText, el, restoreCursor);
  };

  useLayoutEffect(() => {
    if (contentEditableRef.current && value !== undefined) {
      const newValue = String(value);
      setCurrentValue(newValue);
      contentEditableRef.current.innerHTML = highlightMarkdown(newValue);
    }
  }, [value]);

  return (
    <>
      <div
        ref={contentEditableRef}
        className={cn(textAreaStyles.textarea, styles.editor, className)}
        contentEditable
        onInput={handleInput}
        onPaste={handlePaste}
        onKeyDown={handleKeyDown}
        data-placeholder={placeholder}
        data-testid={testId}
        suppressContentEditableWarning={true}
        aria-placeholder={placeholder}
        role="textbox"
        aria-multiline="true"
        style={{
          whiteSpace: "pre-wrap",
          overflowWrap: "break-word",
          ...style,
        }}
      />
      <textarea
        value={currentValue}
        onChange={() => {}} // noop
        placeholder={placeholder}
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          opacity: 0,
          pointerEvents: "none",
          border: "none",
          resize: "none",
          background: "transparent",
        }}
        readOnly
        {...rest}
      />
    </>
  );
};

export default MarkdownEditor;
