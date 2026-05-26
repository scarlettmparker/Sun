import React, { createContext, useContext, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import { cn } from "~/utils/cn";
import "./dialog.module.css";

/**
 * Context value containing shared state for managing visibility and close handlers.
 */
type DialogContextValue = {
  open: boolean;
  setOpen: (open: boolean) => void;
};

/**
 * React context for sharing dialog visibility state.
 */
const DialogContext = createContext<DialogContextValue | null>(null);

/**
 * Internal custom hook for extracting DialogContext parameters safely.
 */
const useDialog = () => {
  const ctx = useContext(DialogContext);
  if (!ctx) {
    throw new Error(
      "Dialog sub-components must be used inside a Dialog component",
    );
  }
  return ctx;
};

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
  /**
   * Callback invoked when the open state changes (e.g., when close buttons are clicked).
   */
  onOpenChange?: (open: boolean) => void;
};

/**
 * Scarlet UI Dialog Component.
 */
const Dialog = (props: DialogProps) => {
  const {
    className,
    children,
    modal = true,
    open = true,
    onOpenChange,
    ...rest
  } = props;
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted || !open) return null;

  const handleClose = () => {
    onOpenChange?.(false);
  };

  const content = (
    <DialogContext.Provider
      value={{ open, setOpen: onOpenChange ?? (() => {}) }}
    >
      <div className={cn("dialog_wrapper", modal && "dialog_modal_active")}>
        {modal && (
          <div
            className="dialog_overlay"
            aria-hidden="true"
            onClick={handleClose}
          />
        )}
        <article
          className={cn("dialog", className)}
          role="dialog"
          aria-modal={modal}
          {...rest}
        >
          <button
            type="button"
            className="dialog_close_button"
            aria-label="Close"
            onClick={handleClose}
          >
            <X className="dialog_close_icon" />
          </button>
          {children}
        </article>
      </div>
    </DialogContext.Provider>
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

type DialogCloseProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  /**
   * Change the underlying rendered element to the direct child.
   */
  asChild?: boolean;
};

/**
 * Scarlet UI Dialog Close Trigger.
 */
const DialogClose = (props: DialogCloseProps) => {
  const { className, children, asChild, onClick, ...rest } = props;
  const { setOpen } = useDialog();

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    onClick?.(event);
    if (!event.defaultPrevented) {
      setOpen(false);
    }
  };

  if (asChild && React.isValidElement(children)) {
    const childProps = children.props as React.HTMLAttributes<HTMLElement>;

    return React.cloneElement(children, {
      ...rest,
      ...childProps,
      onClick: (event: React.MouseEvent<HTMLButtonElement>) => {
        childProps.onClick?.(event);
        handleClick(event);
      },
      className: cn("dialog_close", className, childProps.className),
    } as React.Attributes & React.HTMLAttributes<HTMLElement>);
  }

  return (
    <button
      type="button"
      className={cn("dialog_close", className)}
      onClick={handleClick}
      {...rest}
    >
      {children}
    </button>
  );
};

export default Dialog;
export {
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogBody,
  DialogFooter,
  DialogClose,
};
