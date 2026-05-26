import React, { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { cn } from "~/utils/cn";
import "./dialog.module.css";

type DialogProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Toggles the modal mode. If true, renders a 50% black overlay and traps pointer events.
   * @default true
   */
  modal?: boolean;
  /**
   * Controls the visibility of the dialog.
   * @default true
   */
  open?: boolean;
};

/**
 * Scarlet UI Dialog Component.
 */
const Dialog = (props: DialogProps) => {
  const { className, children, modal = true, open = true, ...rest } = props;
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted || !open) return null;

  const content = (
    <div className={cn("dialog_wrapper", modal && "dialog_modal_active")}>
      {modal && <div className="dialog_overlay" aria-hidden="true" />}
      <article
        className={cn("dialog", className)}
        role="dialog"
        aria-modal={modal}
        {...rest}
      >
        {children}
      </article>
    </div>
  );

  return createPortal(content, document.body);
};

type DialogHeaderProps = React.HTMLAttributes<HTMLElement>;

/**
 * Scarlet UI Dialog Header.
 */
const DialogHeader = (props: DialogHeaderProps) => {
  const { className, children, ...rest } = props;

  return (
    <header className={cn("dialog_header", className)} {...rest}>
      {children}
    </header>
  );
};

type DialogTitleProps = React.HTMLAttributes<HTMLHeadingElement>;

/**
 * Scarlet UI Dialog Title.
 */
const DialogTitle = (props: DialogTitleProps) => {
  const { className, children, ...rest } = props;

  return (
    <h3 className={cn("dialog_title", className)} {...rest}>
      {children}
    </h3>
  );
};

type DialogDescriptionProps = React.HTMLAttributes<HTMLParagraphElement>;

/**
 * Scarlet UI Dialog Description.
 */
const DialogDescription = (props: DialogDescriptionProps) => {
  const { className, children, ...rest } = props;

  return (
    <p className={cn("dialog_description", className)} {...rest}>
      {children}
    </p>
  );
};

type DialogBodyProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Dialog Body.
 */
const DialogBody = (props: DialogBodyProps) => {
  const { className, children, ...rest } = props;

  return (
    <div className={cn("dialog_body", className)} {...rest}>
      {children}
    </div>
  );
};

type DialogFooterProps = React.HTMLAttributes<HTMLElement>;

/**
 * Scarlet UI Dialog Footer.
 */
const DialogFooter = (props: DialogFooterProps) => {
  const { className, children, ...rest } = props;

  return (
    <footer className={cn("dialog_footer", className)} {...rest}>
      {children}
    </footer>
  );
};

export default Dialog;
export {
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogBody,
  DialogFooter,
};
