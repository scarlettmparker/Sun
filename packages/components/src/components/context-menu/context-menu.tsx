import React, {
  cloneElement,
  createContext,
  ReactElement,
  useContext,
  useEffect,
  useId,
  useRef,
  useState,
} from "react";
import { ChevronRight } from "lucide-react";
import { cn } from "~/utils/cn";
import Button from "../button";
import "./context-menu.module.css";

type ContextMenuContextValue = {
  /**
   * Open state for the context menu.
   */
  open: boolean;
  /**
   * Set whether the menu should be open or closed.
   */
  setOpen: (open: boolean) => void;
  /**
   * Toggle the open state.
   */
  toggle: () => void;
  /**
   * Unique id for the trigger button.
   */
  triggerId: string;
  /**
   * Unique id for the content panel.
   */
  contentId: string;
  /**
   * Close the menu.
   */
  close: () => void;
};

const ContextMenuContext = createContext<ContextMenuContextValue | null>(null);

/**
 * ContextMenu provides a shared open state for menu components.
 */
const ContextMenu = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  const [open, setOpenState] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const idBase = useId();
  const triggerId = `context-menu-trigger-${idBase}`;
  const contentId = `context-menu-content-${idBase}`;

  const setOpen = (value: boolean) => setOpenState(value);
  const toggle = () => setOpenState((current) => !current);
  const close = () => setOpenState(false);

  useEffect(() => {
    if (!open) {
      return;
    }

    const handleOutside = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleKeyboard = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutside);
    document.addEventListener("keydown", handleKeyboard);

    return () => {
      document.removeEventListener("mousedown", handleOutside);
      document.removeEventListener("keydown", handleKeyboard);
    };
  }, [open]);

  return (
    <ContextMenuContext.Provider
      value={{ open, setOpen, toggle, triggerId, contentId, close }}
    >
      <div ref={rootRef} className={cn("context_menu", className)} {...rest}>
        {children}
      </div>
    </ContextMenuContext.Provider>
  );
};

const useContextMenu = () => {
  const ctx = useContext(ContextMenuContext);
  if (!ctx) {
    throw new Error("ContextMenu components must be used inside a ContextMenu");
  }

  return ctx;
};

type ContextMenuTriggerProps = React.ComponentProps<typeof Button>;

/**
 * ContextMenuTrigger opens and closes the menu on click.
 */
const ContextMenuTrigger = (props: ContextMenuTriggerProps) => {
  const { children, className, onClick, ...rest } = props;
  const { open, toggle, triggerId, contentId } = useContextMenu();

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    toggle();
    onClick?.(event);
  };

  return (
    <Button
      {...rest}
      id={triggerId}
      aria-haspopup="menu"
      aria-expanded={open}
      aria-controls={contentId}
      className={cn("context_menu_trigger", className)}
      onClick={handleClick}
    >
      {children}
    </Button>
  );
};

type ContextMenuContentProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Alignment side for the menu content.
   */
  side?: "right" | "left";
};

/**
 * ContextMenuContent renders menu contents placed next to the trigger.
 */
const ContextMenuContent = (props: ContextMenuContentProps) => {
  const { className, children, side = "right", ...rest } = props;
  const { open, contentId, triggerId } = useContextMenu();

  if (!open) {
    return null;
  }

  return (
    <div
      {...rest}
      id={contentId}
      role="menu"
      aria-labelledby={triggerId}
      className={cn(
        "context_menu_content",
        side === "left"
          ? "context_menu_content_left"
          : "context_menu_content_right",
        className,
      )}
    >
      {children}
    </div>
  );
};

type ContextMenuItemProps = React.ComponentProps<typeof Button> & {
  /**
   * Render the child element instead of the internal button.
   */
  asChild?: boolean;
  /**
   * Callback after the item is selected.
   */
  onSelect?: () => void;
};

/**
 * ContextMenuItem is a menu action that closes the menu after selection.
 */
const ContextMenuItem = (props: ContextMenuItemProps) => {
  const {
    children,
    className,
    onClick,
    onKeyDown,
    onSelect,
    disabled,
    asChild,
    variant,
    ...rest
  } = props;
  const { close } = useContextMenu();

  /**
   * Handle click or tap activation of the menu item.
   */
  const handleAction = (event: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled) {
      return;
    }

    onClick?.(event);
    onSelect?.();
    close();
  };

  /**
   * Handle keyboard activation of the menu item.
   */
  const handleKey = (event: React.KeyboardEvent<HTMLButtonElement>) => {
    if (disabled) {
      return;
    }

    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onSelect?.();
      close();
    }

    onKeyDown?.(event);
  };

  if (asChild && React.isValidElement(children)) {
    const child = children as ReactElement;
    const {
      type: childType,
      role: childRole,
      disabled: childDisabled,
      className: childClassName,
      onClick: childOnClick,
      onKeyDown: childOnKeyDown,
    } = child.props as Record<string, unknown>;

    return cloneElement(child, {
      ...rest,
      type: (childType as string) ?? "button",
      role: (childRole as string) ?? "menuitem",
      disabled: (childDisabled as boolean) ?? disabled,
      className: cn("context_menu_item", className, childClassName as string),
      onClick: (event: React.MouseEvent<any>) => {
        if (disabled) {
          return;
        }

        (childOnClick as (event: React.MouseEvent<any>) => void)?.(event);
        onClick?.(event);
        onSelect?.();
        close();
      },
      onKeyDown: (event: React.KeyboardEvent<any>) => {
        if (disabled) {
          return;
        }

        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          (childOnKeyDown as (event: React.KeyboardEvent<any>) => void)?.(
            event,
          );
          onSelect?.();
          close();
          return;
        }

        (childOnKeyDown as (event: React.KeyboardEvent<any>) => void)?.(event);
        onKeyDown?.(event);
      },
    } as any);
  }

  return (
    <Button
      {...rest}
      variant={variant ?? "secondary"}
      type="button"
      role="menuitem"
      disabled={disabled}
      className={cn("context_menu_item", className)}
      onClick={handleAction}
      onKeyDown={handleKey}
    >
      {children}
    </Button>
  );
};

/**
 * ContextMenuGroup groups related menu items.
 */
const ContextMenuGroup = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;

  return (
    <div className={cn("context_menu_group", className)} {...rest}>
      {children}
    </div>
  );
};

type ContextMenuSubContextValue = {
  /**
   * Whether the nested sub-menu is open.
   */
  open: boolean;
  /**
   * Set the nested sub-menu open state.
   */
  setOpen: (open: boolean) => void;
  /**
   * Close the nested sub-menu.
   */
  close: () => void;
};

const ContextMenuSubContext = createContext<ContextMenuSubContextValue | null>(
  null,
);

/**
 * ContextMenuSub provides nested sub-menu state.
 */
const ContextMenuSub = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  const [open, setOpenState] = useState(false);
  const close = () => setOpenState(false);

  const setOpen = (value: boolean) => setOpenState(value);

  return (
    <ContextMenuSubContext.Provider value={{ open, setOpen, close }}>
      <div className={cn("context_menu_sub", className)} {...rest}>
        {children}
      </div>
    </ContextMenuSubContext.Provider>
  );
};

const useContextMenuSub = () => {
  const ctx = useContext(ContextMenuSubContext);
  if (!ctx) {
    throw new Error(
      "ContextMenuSub components must be used inside a ContextMenuSub",
    );
  }

  return ctx;
};

type ContextMenuSubTriggerProps = React.ComponentProps<typeof Button>;

/**
 * ContextMenuSubTrigger opens nested sub-menus.
 */
const ContextMenuSubTrigger = (props: ContextMenuSubTriggerProps) => {
  const { children, className, onClick, ...rest } = props;
  const { open, setOpen } = useContextMenuSub();

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setOpen(!open);
    onClick?.(event);
  };

  return (
    <Button
      {...rest}
      variant="secondary"
      type="button"
      aria-haspopup="menu"
      aria-expanded={open}
      className={cn("context_menu_subtrigger", className)}
      onClick={handleClick}
    >
      {children}
      <ChevronRight size={16} className="context_menu_subarrow" />
    </Button>
  );
};

type ContextMenuSubContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * ContextMenuSubContent renders nested menu content.
 */
const ContextMenuSubContent = (props: ContextMenuSubContentProps) => {
  const { className, children, ...rest } = props;
  const { open } = useContextMenuSub();

  if (!open) {
    return null;
  }

  return (
    <div
      {...rest}
      role="menu"
      className={cn("context_menu_subcontent", className)}
    >
      {children}
    </div>
  );
};

export default ContextMenu;
export {
  ContextMenuTrigger,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuGroup,
  ContextMenuSub,
  ContextMenuSubTrigger,
  ContextMenuSubContent,
};
