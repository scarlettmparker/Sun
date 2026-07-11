import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useRef,
} from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import { cn } from "~/utils/cn";
import styles from "./dialog.module.css";

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

type DialogProps = React.HTMLAttributes<HTMLElement> & {
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
    onKeyDown,
    ...rest
  } = props;
  const [mounted, setMounted] = useState(false);
  const dialogRef = useRef<HTMLElement>(null);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Autofocus the submit button when the dialog opens
  useEffect(() => {
    if (open && mounted && dialogRef.current) {
      const timer = setTimeout(() => {
        const submitBtn = dialogRef.current?.querySelector(
          'button[type="submit"]',
        ) as HTMLButtonElement;

        if (submitBtn) {
          submitBtn.focus();
        }
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [open, mounted]);

  if (!mounted || !open) return null;

  const handleClose = () => {
    onOpenChange?.(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented) return;

    if (e.key === "Enter") {
      if (document.activeElement?.tagName === "TEXTAREA") return;

      const submitBtn = e.currentTarget.querySelector(
        'button[type="submit"]',
      ) as HTMLButtonElement;

      if (submitBtn && document.activeElement !== submitBtn) {
        e.preventDefault();
        submitBtn.click();
      }
    }
  };

  const content = (
    <DialogContext.Provider
      value={{ open, setOpen: onOpenChange ?? (() => {}) }}
    >
      <div className={cn(styles.dialog_wrapper, modal && styles.dialog_modal_active)}>
        {modal && (
          <div
            className={styles.dialog_overlay}
            aria-hidden="true"
            onClick={handleClose}
          />
        )}
        <article
          ref={dialogRef}
          className={cn(styles.dialog, className)}
          role={styles.dialog}           aria-modal={modal}
          onKeyDown={handleKeyDown}
          tabIndex={-1}
          {...rest}
        >
          <button
            type="button"
            className={styles.dialog_close_button}
            aria-label="Close"
            onClick={handleClose}
          >
            <X className={styles.dialog_close_icon} />
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
 * Scarlet UI Dialog Header wrapper container.
 */
const DialogHeader = (props: DialogHeaderProps) => {
  const { className, children, ...rest } = props;

  return (
    <header className={cn(styles.dialog_header, className)} {...rest}>
      {children}
    </header>
  );
};

type DialogTitleProps = React.HTMLAttributes<HTMLHeadingElement>;

/**
 * Scarlet UI Dialog Title heading element.
 */
const DialogTitle = (props: DialogTitleProps) => {
  const { className, children, ...rest } = props;

  return (
    <h3 className={cn(styles.dialog_title, className)} {...rest}>
      {children}
    </h3>
  );
};

type DialogDescriptionProps = React.HTMLAttributes<HTMLParagraphElement>;

/**
 * Scarlet UI Dialog Description paragraph element.
 */
const DialogDescription = (props: DialogDescriptionProps) => {
  const { className, children, ...rest } = props;

  return (
    <p className={cn(styles.dialog_description, className)} {...rest}>
      {children}
    </p>
  );
};

type DialogBodyProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Dialog Body structural container.
 */
const DialogBody = (props: DialogBodyProps) => {
  const { className, children, ...rest } = props;

  return (
    <div className={cn(styles.dialog_body, className)} {...rest}>
      {children}
    </div>
  );
};

type DialogFooterProps = React.HTMLAttributes<HTMLElement>;

/**
 * Scarlet UI Dialog Footer actions container.
 */
const DialogFooter = (props: DialogFooterProps) => {
  const { className, children, ...rest } = props;

  return (
    <footer className={cn(styles.dialog_footer, className)} {...rest}>
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
      className: cn(styles.dialog_close, className, childProps.className),
    } as React.Attributes & React.HTMLAttributes<HTMLElement>);
  }

  return (
    <button
      type="button"
      className={cn(styles.dialog_close, className)}
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
