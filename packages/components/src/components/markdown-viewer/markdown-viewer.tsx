import React from "react";
import { cn } from "~/utils/cn";
import { highlightMarkdown, stripMarkdown } from "~/utils/markdown";
import "./markdown-viewer.module.css";

type MarkdownViewerProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * MarkdownViewer component for rendering markdown content.
 */
const MarkdownViewer = (props: MarkdownViewerProps) => {
  const { children, className, ...rest } = props;

  if (typeof children !== "string") {
    throw new Error("MarkdownViewer children must be a string");
  }

  return (
    <div
      className={cn(className, "markdown_viewer")}
      {...rest}
      dangerouslySetInnerHTML={{
        __html: stripMarkdown(highlightMarkdown(children)),
      }}
    />
  );
};

export default MarkdownViewer;
