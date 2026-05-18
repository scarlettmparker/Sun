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

/**
 * X and Y viewport coordinates representing the mouse position during a right-click.
 */
type Position = { x: number; y: number };

/**
 * Context value containing shared state for managing the visibility and
 * viewport anchoring position of the ContextMenu.
 */
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
   * The explicit viewport coordinates where the context menu will render.
   */
  position: Position;
  /**
   * Setter to update the anchor viewport coordinates on right-click.
   */
  setPosition: (position: Position) => void;
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
 * Scarlet Ui Context Menu.
 */
const ContextMenu = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  const [open, setOpenState] = useState(false);
  const [position, setPosition] = useState<Position>({ x: 0, y: 0 });
  const rootRef = useRef<HTMLDivElement>(null);
  const idBase = useId();
  const triggerId = `context-menu-trigger-${idBase}`;
  const contentId = `context-menu-content-${idBase}`;

  const setOpen = (value: boolean) => setOpenState(value);
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
      value={{
        open,
        setOpen,
        position,
        setPosition,
        triggerId,
        contentId,
        close,
      }}
    >
      <div
        ref={rootRef}
        className={cn("context_menu", className)}
        data-state={open ? "open" : "closed"}
        {...rest}
      >
        {children}
      </div>
    </ContextMenuContext.Provider>
  );
};

/**
 * Internal custom hook for verifying and extracting ContextMenuContext parameters safely.
 * @throws {Error} Component must be wrapped inside a parent `<ContextMenu />` node.
 */
const useContextMenu = () => {
  const ctx = useContext(ContextMenuContext);
  if (!ctx) {
    throw new Error("ContextMenu components must be used inside a ContextMenu");
  }

  return ctx;
};

/**
 * Component prop types for the ContextMenuTrigger.
 */
type ContextMenuTriggerProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Render the child element directly instead of an internal unstyled div wrapper.
   */
  asChild?: boolean;
};

/**
 * ContextMenuTrigger wraps target elements to capture right-click cursor locations,
 * intercepts native browser window behaviors, and maps semantic interaction states.
 */
const ContextMenuTrigger = (props: ContextMenuTriggerProps) => {
  const { children, className, onContextMenu, onClick, asChild, ...rest } =
    props;
  const { open, setOpen, setPosition, triggerId, contentId } = useContextMenu();

  const handleContextMenu = (event: React.MouseEvent<HTMLDivElement>) => {
    event.preventDefault();
    if (open) {
      setOpen(false);
    } else {
      setPosition({ x: event.clientX, y: event.clientY });
      setOpen(true);
    }
    onContextMenu?.(event);
  };

  const handleClick = (event: React.MouseEvent<HTMLDivElement>) => {
    if (open) {
      setOpen(false);
    }
    onClick?.(event);
  };

  const sharedProps = {
    id: triggerId,
    "aria-haspopup": "menu" as const,
    "aria-expanded": open,
    "aria-controls": open ? contentId : undefined,
    "data-state": open ? "open" : "closed",
    className: cn("context_menu_trigger", className),
    onContextMenu: handleContextMenu,
    onClick: handleClick,
    ...rest,
  };

  if (asChild && React.isValidElement(children)) {
    const child = children as ReactElement<{ className?: string }>;

    return cloneElement(child, {
      ...sharedProps,
      className: cn(sharedProps.className, child.props.className),
    });
  }

  return <div {...sharedProps}>{children}</div>;
};

/**
 * Component prop types for ContextMenuContent container elements.
 */
type ContextMenuContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * ContextMenuContent mounts content templates onto fixed coordinates relative to the
 * display viewport when triggered by mouse activity.
 */
const ContextMenuContent = (props: ContextMenuContentProps) => {
  const { className, children, style, onKeyDown, ...rest } = props;
  const { open, contentId, triggerId, position } = useContextMenu();
  const contentRef = useRef<HTMLDivElement>(null);

  // Focus the first actionable menu item when the popover opens
  useEffect(() => {
    if (open && contentRef.current) {
      const firstItem = contentRef.current.querySelector(
        '[role="menuitem"]:not([aria-disabled="true"])',
      ) as HTMLElement;
      firstItem?.focus();
    }
  }, [open]);

  // Handle accessible keyboard arrow navigation loops
  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (!contentRef.current) return;

    const items = Array.from(
      contentRef.current.querySelectorAll(
        '[role="menuitem"]:not([aria-disabled="true"])',
      ),
    ) as HTMLElement[];

    const currentIndex = items.indexOf(document.activeElement as HTMLElement);

    if (event.key === "ArrowDown") {
      event.preventDefault();
      const nextIndex = (currentIndex + 1) % items.length;
      items[nextIndex]?.focus();
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      const prevIndex = (currentIndex - 1 + items.length) % items.length;
      items[prevIndex]?.focus();
    }

    onKeyDown?.(event);
  };

  if (!open) {
    return null;
  }

  return (
    <div
      {...rest}
      ref={contentRef}
      id={contentId}
      role="menu"
      aria-label="Context Menu"
      aria-orientation="vertical"
      aria-labelledby={triggerId}
      aria-hidden={!open}
      data-state={open ? "open" : "closed"}
      className={cn("context_menu_content", className)}
      onKeyDown={handleKeyDown}
      style={{
        position: "fixed",
        top: `${position.y}px`,
        left: `${position.x}px`,
        ...style,
      }}
    >
      {children}
    </div>
  );
};

/**
 * Component prop types for context list action elements.
 */
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
 * ContextMenuItem handles interactive row definitions within an open layout view,
 * executing specific functional assignments and dimming contextual overlays when chosen.
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

  const handleAction = (event: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled) {
      return;
    }

    onClick?.(event);
    onSelect?.();
    close();
  };

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
      tabIndex: -1,
      "aria-disabled": (childDisabled as boolean) ?? disabled,
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
      tabIndex={-1}
      disabled={disabled}
      aria-disabled={disabled}
      className={cn("context_menu_item", className)}
      onClick={handleAction}
      onKeyDown={handleKey}
    >
      {children}
    </Button>
  );
};

/**
 * ContextMenuGroup groups related menu items together visually and semantically.
 */
const ContextMenuGroup = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;

  return (
    <div className={cn("context_menu_group", className)} role="group" {...rest}>
      {children}
    </div>
  );
};

/**
 * Nested sub-menu state values passing visibility controls downstream.
 */
type ContextMenuSubContextValue = {
  /**
   * Whether the nested sub-menu is currently open.
   */
  open: boolean;
  /**
   * Set the nested sub-menu open state flag.
   */
  setOpen: (open: boolean) => void;
  /**
   * Close the nested sub-menu.
   */
  close: () => void;
};

/**
 * Context provider pipeline managing layout and toggle properties for sub-levels.
 */
const ContextMenuSubContext = createContext<ContextMenuSubContextValue | null>(
  null,
);

/**
 * ContextMenuSub provides isolated visibility state environments for multi-tier,
 * fly-out nested submenu trees.
 */
const ContextMenuSub = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  const [open, setOpenState] = useState(false);
  const close = () => setOpenState(false);
  const setOpen = (value: boolean) => setOpenState(value);

  return (
    <ContextMenuSubContext.Provider value={{ open, setOpen, close }}>
      <div
        className={cn("context_menu_sub", className)}
        data-state={open ? "open" : "closed"}
        {...rest}
      >
        {children}
      </div>
    </ContextMenuSubContext.Provider>
  );
};

/**
 * Internal safe state custom hook variant used for managing deep contextual submenu hooks.
 * @throws {Error} ContextMenuSub components must be encapsulated inside sub-trees.
 */
const useContextMenuSub = () => {
  const ctx = useContext(ContextMenuSubContext);
  if (!ctx) {
    throw new Error(
      "ContextMenuSub components must be used inside a ContextMenuSub",
    );
  }

  return ctx;
};

/**
 * Prop mappings matching ContextMenuSubTrigger button properties.
 */
type ContextMenuSubTriggerProps = React.ComponentProps<typeof Button>;

/**
 * ContextMenuSubTrigger acts as a bridge interactive row element that expands
 * adjacent child sub-content popovers upon interaction.
 */
const ContextMenuSubTrigger = (props: ContextMenuSubTriggerProps) => {
  const { children, className, onClick, disabled, ...rest } = props;
  const { open, setOpen } = useContextMenuSub();

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled) return;
    setOpen(!open);
    onClick?.(event);
  };

  return (
    <Button
      {...rest}
      variant="secondary"
      type="button"
      role="menuitem"
      tabIndex={-1}
      aria-haspopup="menu"
      aria-expanded={open}
      disabled={disabled}
      aria-disabled={disabled}
      className={cn("context_menu_subtrigger", className)}
      onClick={handleClick}
    >
      {children}
      <ChevronRight size={16} className="context_menu_subarrow" />
    </Button>
  );
};

/**
 * Structural type definitions for nesting contextual content panels.
 */
type ContextMenuSubContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * ContextMenuSubContent handles rendering nested secondary flyout card modules
 * adjacent to active sub-triggers.
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
      aria-orientation="vertical"
      aria-hidden={!open}
      data-state={open ? "open" : "closed"}
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
