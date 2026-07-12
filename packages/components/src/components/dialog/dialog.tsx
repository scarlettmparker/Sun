import React, {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
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
   * Makes the dialog draggable by its header. Disables modal mode (no overlay).
   * @default false
   */
  draggable?: boolean;
  /**
   * Controls the visibility of the dialog.
   * @default true
   */
  open?: boolean;
  /**
   * Callback invoked when the open state changes (e.g., when close buttons are clicked).
   */
  onOpenChange?: (open: boolean) => void;
  /**
   * Initial position for draggable dialogs.
   */
  position?: { top: number; left: number };
};

let dragZCounter = 100;

/**
 * Scarlet UI Dialog Component.
 */
const Dialog = (props: DialogProps) => {
  const {
    className,
    children,
    modal = true,
    draggable = false,
    open = true,
    onOpenChange,
    position,
    onKeyDown,
    ...rest
  } = props;
  const [mounted, setMounted] = useState(false);
  const dialogRef = useRef<HTMLElement>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [dragOffset, setDragOffset] = useState<{ x: number; y: number } | null>(null);
  const [dragPos, setDragPos] = useState(position ?? { top: 100, left: 100 });

  useEffect(() => {
    setMounted(true);
  }, []);

  /**
   * Resets the dragged position only when the position value actually changes,
   * so re-renders that pass a new object (e.g. typing in a child input) don't
   * snap the dialog back to its origin.
   */
  const lastPosition = useRef(position);
  useEffect(() => {
    if (
      position &&
      (position.top !== lastPosition.current?.top ||
        position.left !== lastPosition.current?.left)
    ) {
      setDragPos(position);
    }
    lastPosition.current = position;
  }, [position]);

  /**
   * Autofocus the submit button when the dialog opens.
   */
  useEffect(() => {
    if (open && mounted && dialogRef.current && !draggable) {
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
  }, [open, mounted, draggable]);

  /**
   * Brings a draggable dialog to the front when it opens.
   */
  useEffect(() => {
    if (open && mounted && draggable) {
      bringToFront();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, mounted, draggable]);

  /**
   * Keeps a draggable dialog inside the viewport when the window resizes.
   */
  useEffect(() => {
    if (!draggable) return;
    const handleResize = () => {
      const el = dialogRef.current;
      if (!el) return;
      setDragPos((pos) => ({
        top: Math.max(0, Math.min(pos.top, window.innerHeight - el.offsetHeight)),
        left: Math.max(0, Math.min(pos.left, window.innerWidth - el.offsetWidth)),
      }));
    };
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, [draggable]);

  /**
   * Updates the dialog position as the mouse moves during a drag.
   */
  useEffect(() => {
    if (!dragOffset) return;

    const handleMouseMove = (e: MouseEvent) => {
      setDragPos({
        top: e.clientY - dragOffset.y,
        left: e.clientX - dragOffset.x,
      });
    };

    const handleMouseUp = () => {
      setDragOffset(null);
    };

    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", handleMouseUp);
    return () => {
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);
    };
  }, [dragOffset]);

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

  /**
   * Brings this dialog's wrapper in front of other open dialogs by giving it the
   * next z-index. The wrapper (not the article) must carry the z-index, since the
   * article is trapped inside the wrapper's stacking context.
   */
  const bringToFront = () => {
    dragZCounter += 1;
    if (wrapperRef.current) {
      wrapperRef.current.style.zIndex = String(dragZCounter);
    }
  };

  /**
   * Starts dragging: records the offset between the mouse and the dialog's
   * top-left. Ignored when the press begins on an interactive element (editor,
   * input, button) so resizing the editor or clicking controls doesn't drag.
   */
  const handleDragStart = (e: React.MouseEvent<HTMLElement>) => {
    if (!draggable) return;
    const target = e.target as HTMLElement | null;
    if (
      target?.closest(
        "input, textarea, select, button, a, [contenteditable], [data-no-drag]",
      )
    ) {
      return;
    }
    const rect = e.currentTarget.getBoundingClientRect();
    setDragOffset({ x: e.clientX - rect.left, y: e.clientY - rect.top });
    bringToFront();
  };

  const isModal = modal && !draggable;

  const content = (
    <DialogContext.Provider
      value={{ open, setOpen: onOpenChange ?? (() => {}) }}
    >
      <div
        ref={wrapperRef}
        className={cn(styles.dialog_wrapper, isModal && styles.dialog_modal_active)}
      >
        {isModal && (
          <div
            className={styles.dialog_overlay}
            aria-hidden="true"
            onClick={handleClose}
          />
        )}
        <article
          ref={dialogRef}
          className={cn(
            styles.dialog,
            draggable && styles.dialog_draggable,
            className,
          )}
          role="dialog"
          aria-modal={isModal}
          onKeyDown={handleKeyDown}
          tabIndex={-1}
          onMouseDown={draggable ? handleDragStart : undefined}
          style={draggable ? {
            top: `${dragPos.top}px`,
            left: `${dragPos.left}px`,
          } : undefined}
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
 * Scarlet UI Dialog Header wrapper container. Acts as the drag handle when the
 * dialog is draggable.
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
