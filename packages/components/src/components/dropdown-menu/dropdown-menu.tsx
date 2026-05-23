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
import "./dropdown-menu.module.css";

/**
 * Context value containing shared state for managing the visibility and
 * trigger anchoring for the DropdownMenu.
 */
type DropdownMenuContextValue = {
  /**
   * Open state for the dropdown menu.
   */
  open: boolean;
  /**
   * Set whether the menu should be open or closed.
   */
  setOpen: (open: boolean) => void;
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
  /**
   * Mutable ref to the trigger element used for positioning calculations.
   */
  triggerRef: React.MutableRefObject<HTMLElement | null>;
  /**
   * Nonce incremented on resize/scroll to force repositioning of open content.
   */
  positionNonce: number;
};

/**
 * React context for sharing dropdown menu state (open, trigger ref, ids, etc.).
 */
const DropdownMenuContext = createContext<DropdownMenuContextValue | null>(null);

/**
 * Isomorphic layout effect that uses useLayoutEffect on the client and falls back
 * to useEffect during SSR to avoid warnings.
 */
const useIsomorphicLayoutEffect =
  typeof window !== "undefined" ? useLayoutEffect : useEffect;

/**
 * Scarlet Ui Dropdown Menu.
 */
const DropdownMenu = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  const [open, setOpenState] = useState(false);
  const [resetNonce, setResetNonce] = useState(0);
  const [positionNonce, setPositionNonce] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLElement | null>(null);

  const idBase = useId();
  const triggerId = `dropdown-menu-trigger-${idBase}`;
  const contentId = `dropdown-menu-content-${idBase}`;

  const setOpen = (value: boolean) => {
    if (value) {
      setResetNonce((prev) => prev + 1);
      setPositionNonce((prev) => prev + 1);
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
      // Ignore clicks inside any rendered dropdown menu content/portals
      if (target.closest?.('[data-dropdown-menu-content="true"]')) {
        return;
      }
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
      setPositionNonce((prev) => prev + 1);
    };

    const handleScroll = () => {
      setPositionNonce((prev) => prev + 1);
    };

    document.addEventListener("pointerdown", handleOutside);
    document.addEventListener("keydown", handleKeyboard);
    window.addEventListener("resize", handleResize);
    window.addEventListener("scroll", handleScroll, true);
    return () => {
      document.removeEventListener("pointerdown", handleOutside);
      document.removeEventListener("keydown", handleKeyboard);
      window.removeEventListener("resize", handleResize);
      window.removeEventListener("scroll", handleScroll, true);
    };
  }, [open]);

  return (
    <DropdownMenuContext.Provider
      value={{
        open,
        setOpen,
        triggerId,
        contentId,
        close,
        resetNonce,
        triggerRef,
        positionNonce,
      }}
    >
      <div
        ref={rootRef}
        className={cn("dropdown_menu", className)}
        data-state={open ? "open" : "closed"}
        {...rest}
      >
        {children}
      </div>
    </DropdownMenuContext.Provider>
  );
};

/**
 * Internal custom hook for verifying and extracting DropdownMenuContext parameters safely.
 * @throws {Error} Component must be wrapped inside a parent `<DropdownMenu />` node.
 */
const useDropdownMenu = () => {
  const ctx = useContext(DropdownMenuContext);
  if (!ctx) {
    throw new Error("DropdownMenu components must be used inside a DropdownMenu");
  }
  return ctx;
};

/**
 * Component prop types for the DropdownMenuTrigger.
 */
type DropdownMenuTriggerProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Render the child element directly instead of an internal unstyled div wrapper.
   */
  asChild?: boolean;
};

/**
 * DropdownMenuTrigger wraps target elements to capture click events,
 * toggle the dropdown visibility, and provide ARIA attributes.
 */
const DropdownMenuTrigger = (props: DropdownMenuTriggerProps) => {
  const { children, className, onClick, asChild, ...rest } = props;
  const { open, setOpen, triggerId, contentId, triggerRef } = useDropdownMenu();

  const handleClick = (event: React.MouseEvent<HTMLDivElement>) => {
    if (open) {
      setOpen(false);
    } else {
      triggerRef.current = event.currentTarget;
      setOpen(true);
    }
    onClick?.(event);
  };

  const sharedProps = {
    id: triggerId,
    "aria-haspopup": "menu" as const,
    "aria-expanded": open,
    "aria-controls": open ? contentId : undefined,
    "data-state": open ? "open" : "closed",
    className: cn("dropdown_menu_trigger", className),
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
 * Component prop types for DropdownMenuContent container elements.
 */
type DropdownMenuContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * DropdownMenuContent mounts the menu content using a portal, positioned
 * relative to the trigger element with viewport-aware flipping.
 */
const DropdownMenuContent = (props: DropdownMenuContentProps) => {
  const { className, children, style, onKeyDown, ...rest } = props;
  const { open, contentId, triggerId, triggerRef, positionNonce } =
    useDropdownMenu();
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

  useIsomorphicLayoutEffect(() => {
    if (!open || !contentRef.current || !triggerRef.current) return;

    const updatePosition = () => {
      const triggerRect = triggerRef.current!.getBoundingClientRect();
      const contentEl = contentRef.current!;
      const contentRect = contentEl.getBoundingClientRect();

      const gap = 4;
      let top = triggerRect.bottom + gap;
      let left = triggerRect.left;

      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;

      if (contentRect.width > 0) {
        if (left + contentRect.width > viewportWidth) {
          left = Math.max(gap, viewportWidth - contentRect.width - gap);
        }
      }

      if (contentRect.height > 0) {
        if (top + contentRect.height > viewportHeight) {
          top = Math.max(gap, triggerRect.top - contentRect.height - gap);
        }
      }

      contentEl.style.position = "fixed";
      contentEl.style.top = `${top}px`;
      contentEl.style.left = `${left}px`;
      contentEl.style.zIndex = "201";
    };

    updatePosition();
    const raf = requestAnimationFrame(updatePosition);
    return () => cancelAnimationFrame(raf);
  }, [open, positionNonce]);

  const content = (
    <div
      {...rest}
      ref={contentRef}
      id={contentId}
      role="menu"
      aria-label="Dropdown Menu"
      aria-orientation="vertical"
      aria-labelledby={triggerId}
      aria-hidden={!open}
      data-state={open ? "open" : "closed"}
      data-dropdown-menu-content="true"
      className={cn("dropdown_menu_content", className)}
      onKeyDown={handleKeyDown}
      onPointerDown={(e) => {
        e.stopPropagation();
        rest.onPointerDown?.(e);
      }}
      onClick={(e) => {
        e.stopPropagation();
        rest.onClick?.(e);
      }}
      style={style}
    >
      {children}
    </div>
  );

  return createPortal(content, document.body);
};

/**
 * Component prop types for dropdown list action elements.
 */
type DropdownMenuItemProps = React.ComponentProps<typeof Button> & {
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
 * DropdownMenuItem handles interactive row definitions within an open dropdown,
 * executing specific functional assignments and closing the menu when chosen.
 */
const DropdownMenuItem = (props: DropdownMenuItemProps) => {
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
  const { close } = useDropdownMenu();

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
        "dropdown_menu_item",
        className,
        childProps.className as string,
      ),
      onClick: (event: React.MouseEvent<HTMLButtonElement>) => {
        if (disabled) {
          return;
        }
        (childProps.onClick as (event: React.MouseEvent<HTMLButtonElement>) => void)?.(event);
        onClick?.(event);
        onSelect?.();
        close();
      },
      onKeyDown: (event: React.KeyboardEvent<HTMLButtonElement>) => {
        if (disabled) {
          return;
        }
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          (childProps.onKeyDown as (event: React.KeyboardEvent<HTMLButtonElement>) => void)?.(
            event,
          );
          onSelect?.();
          close();
          return;
        }
        (childProps.onKeyDown as (event: React.KeyboardEvent<HTMLButtonElement>) => void)?.(
          event,
        );
        onKeyDown?.(event);
      },
    } as React.Attributes);
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
      className={cn("dropdown_menu_item", className)}
      onClick={handleAction}
      onKeyDown={handleKey}
    >
      {children}
    </Button>
  );
};

/**
 * DropdownMenuGroup groups related menu items together visually and semantically.
 */
const DropdownMenuGroup = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  return (
    <div className={cn("dropdown_menu_group", className)} role="group" {...rest}>
      {children}
    </div>
  );
};

/**
 * Nested sub-menu state values passing visibility controls downstream.
 */
type DropdownMenuSubContextValue = {
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
const DropdownMenuSubContext = createContext<DropdownMenuSubContextValue | null>(
  null,
);

/**
 * DropdownMenuSub provides isolated visibility state environments for multi-tier,
 * fly-out nested submenu trees.
 */
const DropdownMenuSub = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  const { resetNonce } = useDropdownMenu();
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement | null>(null);

  // Auto-close open submenu if user opens another menu instance
  useEffect(() => {
    setOpen(false);
  }, [resetNonce]);

  return (
    <DropdownMenuSubContext.Provider value={{ open, setOpen, triggerRef }}>
      <div
        className={cn("dropdown_menu_sub", className)}
        data-state={open ? "open" : "closed"}
        {...rest}
      >
        {children}
      </div>
    </DropdownMenuSubContext.Provider>
  );
};

/**
 * Internal safe state custom hook variant used for managing deep contextual submenu hooks.
 * @throws {Error} DropdownMenuSub components must be encapsulated inside sub-trees.
 */
const useDropdownMenuSub = () => {
  const ctx = useContext(DropdownMenuSubContext);
  if (!ctx) {
    throw new Error(
      "DropdownMenuSub components must be used inside a DropdownMenuSub",
    );
  }
  return ctx;
};

/**
 * Prop mappings matching DropdownMenuSubTrigger button properties.
 */
type DropdownMenuSubTriggerProps = React.ComponentProps<typeof Button>;

/**
 * DropdownMenuSubTrigger acts as a bridge interactive row element that expands
 * adjacent child sub-content popovers upon interaction.
 */
const DropdownMenuSubTrigger = (props: DropdownMenuSubTriggerProps) => {
  const {
    children,
    className,
    onPointerEnter,
    onPointerLeave,
    disabled,
    ...rest
  } = props;
  const { open, setOpen, triggerRef } = useDropdownMenuSub();

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
      className={cn("dropdown_menu_subtrigger", className)}
      onPointerEnter={handlePointerEnter}
      onPointerLeave={handlePointerLeave}
    >
      {children}
      <ChevronRight size={16} className="dropdown_menu_subarrow" />
    </Button>
  );
};

/**
 * Structural type definitions for nesting dropdown content panels.
 */
type DropdownMenuSubContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * DropdownMenuSubContent handles rendering nested secondary flyout card modules
 * adjacent to active sub-triggers.
 */
const DropdownMenuSubContent = (props: DropdownMenuSubContentProps) => {
  const { className, children, onPointerEnter, onPointerLeave, ...rest } =
    props;
  const { open, setOpen, triggerRef } = useDropdownMenuSub();
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
      data-dropdown-menu-content="true"
      className={cn("dropdown_menu_subcontent", className)}
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

export default DropdownMenu;
export {
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuGroup,
  DropdownMenuSub,
  DropdownMenuSubTrigger,
  DropdownMenuSubContent,
};
