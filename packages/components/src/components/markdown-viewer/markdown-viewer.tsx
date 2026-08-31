import React from "react";
import { cn } from "~/utils/cn";
import { renderMarkdown } from "~/utils/markdown";
import styles from "./markdown-viewer.module.css";

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
      lang="en"
      className={cn(className, styles.markdown_viewer)}
      {...rest}
      dangerouslySetInnerHTML={{
        __html: renderMarkdown(children),
      }}
    />
  );
};

export default MarkdownViewer;
