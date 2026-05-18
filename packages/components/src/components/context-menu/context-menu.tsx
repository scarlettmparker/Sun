import React, {
  cloneElement,
  createContext,
  ReactElement,
  useContext,
  useEffect,
  useLayoutEffect,
  useId,
  useRef,
  useState,
} from "react";
import { createPortal } from "react-dom";
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
  /**
   * Counter tracking raw activation events used to wipe stale sub-menu traces.
   */
  resetNonce: number;
};

const ContextMenuContext = createContext<ContextMenuContextValue | null>(null);

// Isomorphic layout effect to avoid SSR warnings
const useIsomorphicLayoutEffect =
  typeof window !== "undefined" ? useLayoutEffect : useEffect;

/**
 * Scarlet Ui Context Menu.
 */
const ContextMenu = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  const [open, setOpenState] = useState(false);
  const [position, setPosition] = useState<Position>({ x: 0, y: 0 });
  const [resetNonce, setResetNonce] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);

  // Cache the viewport metrics at the exact moment the menu is triggered open
  const lastWindowSize = useRef({ w: 0, h: 0 });
  const idBase = useId();
  const triggerId = `context-menu-trigger-${idBase}`;
  const contentId = `context-menu-content-${idBase}`;

  const setOpen = (value: boolean) => {
    if (value) {
      lastWindowSize.current = { w: window.innerWidth, h: window.innerHeight };
      // Force nested submenus to drop open states when a fresh menu anchor triggers
      setResetNonce((prev) => prev + 1);
    }
    setOpenState(value);
  };

  const close = () => setOpenState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    const handleOutside = (event: PointerEvent) => {
      const target = event.target as HTMLElement;
      if (rootRef.current && rootRef.current.contains(target)) {
        return;
      }
      // Ignore clicks inside any rendered context menu content/portals
      if (target.closest?.('[data-context-menu-content="true"]')) {
        return;
      }
      setOpenState(false);
    };

    const handleKeyboard = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpenState(false);
      }
    };

    const handleResize = () => {
      const currentW = window.innerWidth;
      const currentH = window.innerHeight;
      // Determine exactly how many pixels the boundaries translated
      const deltaX = currentW - lastWindowSize.current.w;
      const deltaY = currentH - lastWindowSize.current.h;
      if (deltaX !== 0 || deltaY !== 0) {
        setPosition((prev) => ({
          x: prev.x + deltaX,
          y: prev.y + deltaY,
        }));
        // Document the new dimension baseline for sequential resize events
        lastWindowSize.current = { w: currentW, h: currentH };
      }
    };

    document.addEventListener("pointerdown", handleOutside);
    document.addEventListener("keydown", handleKeyboard);
    window.addEventListener("resize", handleResize);
    return () => {
      document.removeEventListener("pointerdown", handleOutside);
      document.removeEventListener("keydown", handleKeyboard);
      window.removeEventListener("resize", handleResize);
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
        resetNonce,
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
    setPosition({ x: event.clientX, y: event.clientY });
    setOpen(true);
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
    const child = children as ReactElement<React.HTMLAttributes<HTMLElement>>;
    const childProps = child.props as { className?: string };
    return cloneElement(child, {
      ...sharedProps,
      className: cn(sharedProps.className, childProps.className),
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

  useEffect(() => {
    if (open && contentRef.current) {
      const firstItem = contentRef.current.querySelector(
        '[role="menuitem"]:not([aria-disabled="true"])',
      ) as HTMLElement;
      firstItem?.focus();
    }
  }, [open]);

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

  const content = (
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
      data-context-menu-content="true"
      className={cn("context_menu_content", className)}
      onKeyDown={handleKeyDown}
      onPointerDown={(e) => {
        e.stopPropagation();
        rest.onPointerDown?.(e);
      }}
      onClick={(e) => {
        e.stopPropagation();
        rest.onClick?.(e);
      }}
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

  return createPortal(content, document.body);
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
    const childProps = child.props as Record<string, unknown>;
    return cloneElement(child, {
      ...rest,
      type: (childProps.type as string) ?? "button",
      role: (childProps.role as string) ?? "menuitem",
      tabIndex: -1,
      "aria-disabled": (childProps.disabled as boolean) ?? disabled,
      className: cn(
        "context_menu_item",
        className,
        childProps.className as string,
      ),
      onClick: (event: React.MouseEvent<any>) => {
        if (disabled) {
          return;
        }
        (childProps.onClick as (event: React.MouseEvent<any>) => void)?.(event);
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
          (childProps.onKeyDown as (event: React.KeyboardEvent<any>) => void)?.(
            event,
          );
          onSelect?.();
          close();
          return;
        }
        (childProps.onKeyDown as (event: React.KeyboardEvent<any>) => void)?.(
          event,
        );
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
   * Mutable ref passing the trigger element for stable contextual alignment.
   */
  triggerRef: React.MutableRefObject<HTMLButtonElement | null>;
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
  const { resetNonce } = useContextMenu();
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement | null>(null);

  // Auto-close open submenu if user right-clicks another item to re-anchor the main context menu
  useEffect(() => {
    setOpen(false);
  }, [resetNonce]);

  return (
    <ContextMenuSubContext.Provider value={{ open, setOpen, triggerRef }}>
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
  const {
    children,
    className,
    onPointerEnter,
    onPointerLeave,
    disabled,
    ...rest
  } = props;
  const { open, setOpen, triggerRef } = useContextMenuSub();

  const handlePointerEnter = (event: React.PointerEvent<HTMLButtonElement>) => {
    if (disabled) return;
    triggerRef.current = event.currentTarget;
    setOpen(true);
    onPointerEnter?.(event);
  };

  const handlePointerLeave = (event: React.PointerEvent<HTMLButtonElement>) => {
    if (disabled) return;
    setOpen(false);
    onPointerLeave?.(event);
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
      onPointerEnter={handlePointerEnter}
      onPointerLeave={handlePointerLeave}
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
  const { className, children, onPointerEnter, onPointerLeave, ...rest } =
    props;
  const { open, setOpen, triggerRef } = useContextMenuSub();
  const subRef = useRef<HTMLDivElement>(null);

  // Positioning for submenu leveraging Layout effect to prevent 0-dimension blips
  useIsomorphicLayoutEffect(() => {
    if (!open || !subRef.current || !triggerRef.current) return;

    const updatePosition = () => {
      const triggerRect = triggerRef.current!.getBoundingClientRect();
      const submenuRect = subRef.current!.getBoundingClientRect();
      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;

      const gap = -4;
      let left = triggerRect.right + gap;
      let top = triggerRect.top;

      // Horizontal flip if running out of screen width
      if (left + submenuRect.width > viewportWidth) {
        left = triggerRect.left - submenuRect.width - gap;
      }

      // Vertical flip if running off the bottom edge
      if (top + submenuRect.height > viewportHeight) {
        top = triggerRect.bottom - submenuRect.height;
      }

      // Apply final bounds enforcing margin
      subRef.current!.style.position = "fixed";
      subRef.current!.style.top = `${Math.max(gap * 2, top)}px`;
      subRef.current!.style.left = `${Math.max(gap * 2, left)}px`;
      subRef.current!.style.zIndex = "9999";
    };

    updatePosition();
    const raf = requestAnimationFrame(updatePosition);
    return () => cancelAnimationFrame(raf);
  }, [open]);

  const handlePointerEnter = (event: React.PointerEvent<HTMLDivElement>) => {
    setOpen(true);
    onPointerEnter?.(event);
  };

  const handlePointerLeave = (event: React.PointerEvent<HTMLDivElement>) => {
    setOpen(false);
    onPointerLeave?.(event);
  };

  if (!open) {
    return null;
  }

  const content = (
    <div
      {...rest}
      ref={subRef}
      role="menu"
      aria-orientation="vertical"
      aria-hidden={!open}
      data-state={open ? "open" : "closed"}
      data-context-menu-content="true"
      className={cn("context_menu_subcontent", className)}
      onPointerEnter={handlePointerEnter}
      onPointerLeave={handlePointerLeave}
      onPointerDown={(e) => {
        e.stopPropagation();
        rest.onPointerDown?.(e);
      }}
      onClick={(e) => {
        e.stopPropagation();
        rest.onClick?.(e);
      }}
    >
      {children}
    </div>
  );

  return createPortal(content, document.body);
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
