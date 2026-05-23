import React, {
  cloneElement,
  createContext,
  ReactElement,
  useContext,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { createPortal } from "react-dom";
import { ChevronRight } from "lucide-react";
import { cn } from "~/utils/cn";
import Button from "../button";

// Isomorphic layout effect to avoid SSR warnings
const useIsomorphicLayoutEffect =
  typeof window !== "undefined" ? useLayoutEffect : useEffect;

export type MenuItemProps = React.ComponentProps<typeof Button> & {
  asChild?: boolean;
  onSelect?: () => void;
  /**
   * Function provided by the parent menu to close the menu.
   */
  closeMenu: () => void;
};

/**
 * MenuItem handles interactive row definitions within an open menu.
 */
const MenuItem = (props: MenuItemProps) => {
  const {
    children,
    className,
    onClick,
    onKeyDown,
    onSelect,
    disabled,
    asChild,
    variant,
    closeMenu,
    ...rest
  } = props;

  const handleAction = (event: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled) {
      return;
    }
    onClick?.(event);
    onSelect?.();
    closeMenu();
  };

  const handleKey = (event: React.KeyboardEvent<HTMLButtonElement>) => {
    if (disabled) {
      return;
    }
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onSelect?.();
      closeMenu();
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
        closeMenu();
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
          closeMenu();
          return;
        }
        (childProps.onKeyDown as (event: React.KeyboardEvent<HTMLButtonElement>) => void)?.(
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
      className={className}
      onClick={handleAction}
      onKeyDown={handleKey}
    >
      {children}
    </Button>
  );
};

type MenuSubContextValue = {
  open: boolean;
  setOpen: (open: boolean) => void;
  triggerRef: React.MutableRefObject<HTMLButtonElement | null>;
};

const MenuSubContext = createContext<MenuSubContextValue | null>(null);

/**
 * Internal hook for MenuSub descendants.
 * @throws {Error} Must be used inside MenuSub.
 */
const useMenuSub = () => {
  const ctx = useContext(MenuSubContext);
  if (!ctx) throw new Error("MenuSub components must be used inside a MenuSub");
  return ctx;
};

export type MenuSubProps = React.HTMLAttributes<HTMLDivElement> & {
  resetNonce: number;
};

/**
 * MenuSub provides isolated visibility state environments for multi-tier,
 * fly-out nested submenu trees.
 */
const MenuSub = (props: MenuSubProps) => {
  const { className, children, resetNonce, ...rest } = props;
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement | null>(null);

  // Auto-close open submenu when the parent menu's resetNonce changes
  useEffect(() => {
    setOpen(false);
  }, [resetNonce]);

  return (
    <MenuSubContext.Provider value={{ open, setOpen, triggerRef }}>
      <div
        className={className}
        data-state={open ? "open" : "closed"}
        {...rest}
      >
        {children}
      </div>
    </MenuSubContext.Provider>
  );
};

export type MenuSubTriggerProps = React.ComponentProps<typeof Button> & {
  arrowClassName: string;
};

/**
 * MenuSubTrigger acts as a bridge interactive row element that expands
 * adjacent child sub-content popovers upon interaction.
 */
const MenuSubTrigger = (props: MenuSubTriggerProps) => {
  const {
    children,
    className,
    onPointerEnter,
    onPointerLeave,
    disabled,
    arrowClassName,
    ...rest
  } = props;
  const { open, setOpen, triggerRef } = useMenuSub();

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
      className={className}
      onPointerEnter={handlePointerEnter}
      onPointerLeave={handlePointerLeave}
    >
      {children}
      <ChevronRight size={16} className={arrowClassName} />
    </Button>
  );
};

export type MenuSubContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * MenuSubContent handles rendering nested secondary flyout card modules
 * adjacent to active sub-triggers.
 */
const MenuSubContent = (props: MenuSubContentProps) => {
  const {
    className,
    children,
    onPointerEnter,
    onPointerLeave,
    ...rest
  } = props;
  const { open, setOpen, triggerRef } = useMenuSub();
  const subRef = useRef<HTMLDivElement>(null);

  // Isomorphic layout effect to avoid SSR warnings
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
      data-dropdown-menu-content="true"
      className={className}
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

/**
 * Hook to handle viewport-aware positioning + flipping for the main dropdown content.
 * Extracted here so the large positioning block is not repeated in dropdown-menu.tsx.
 */
const useDropdownPositioning = (
  contentRef: React.RefObject<HTMLDivElement | null>,
  triggerRef: React.MutableRefObject<HTMLElement | null>,
  open: boolean,
  positionNonce: number,
) => {
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
};

/**
 * Shared keyboard navigation hook for arrow up/down focusing of menu items.
 * Used internally to avoid duplication in ContextMenuContent / DropdownMenuContent.
 */
const useMenuKeyboardNav = (
  contentRef: React.RefObject<HTMLDivElement | null>,
  open: boolean,
  onKeyDown?: (event: React.KeyboardEvent<HTMLDivElement>) => void,
) => {
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

  return { handleKeyDown };
};

/**
 * Internal shared trigger renderer used by ContextMenuTrigger and DropdownMenuTrigger.
 * Handles asChild cloning vs div wrapper with common ARIA/class merging.
 */
const MenuTrigger = (
  props: React.HTMLAttributes<HTMLDivElement> & {
    asChild?: boolean;
  },
) => {
  const { children, className, asChild, ...rest } = props;
  const sharedProps = {
    ...rest,
    className,
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
 * Internal shared content renderer used by Context*Content and Dropdown*Content.
 */
const MenuContent = (
  props: React.HTMLAttributes<HTMLDivElement> & {
    open: boolean;
    contentRef: React.RefObject<HTMLDivElement | null>;
    id: string;
    triggerId: string;
    ariaLabel: string;
    contentDataAttr: string; // e.g. "context-menu-content" → data-context-menu-content="true"
    style?: React.CSSProperties;
  },
) => {
  const {
    open,
    contentRef,
    id,
    triggerId,
    ariaLabel,
    contentDataAttr,
    className,
    children,
    style,
    onKeyDown: userOnKeyDown,
    ...rest
  } = props;

  const { handleKeyDown } = useMenuKeyboardNav(contentRef, open, userOnKeyDown);

  if (!open) {
    return null;
  }

  const dataAttrName = `data-${contentDataAttr}`;

  const content = (
    <div
      {...rest}
      ref={contentRef}
      id={id}
      role="menu"
      aria-label={ariaLabel}
      aria-orientation="vertical"
      aria-labelledby={triggerId}
      aria-hidden={!open}
      data-state={open ? "open" : "closed"}
      {...{ [dataAttrName]: "true" }}
      className={className}
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
 * Generates stable ids for trigger and content elements for a menu instance.
 */
const useMenuIds = (prefix: "context-menu" | "dropdown-menu") => {
  const idBase = useId();
  return {
    triggerId: `${prefix}-trigger-${idBase}`,
    contentId: `${prefix}-content-${idBase}`,
  };
};

/**
 * Shared effect for closing the menu on outside clicks.
 */
const useMenuCloseHandlers = (
  open: boolean,
  close: () => void,
  rootRef: React.RefObject<HTMLDivElement | null>,
  ignoreSelectors: string[],
) => {
  useEffect(() => {
    if (!open) {
      return;
    }
    const handleOutside = (event: PointerEvent) => {
      const target = event.target as HTMLElement;
      if (rootRef.current && rootRef.current.contains(target)) {
        return;
      }
      for (const sel of ignoreSelectors) {
        if (target.closest?.(sel)) {
          return;
        }
      }
      close();
    };

    const handleKeyboard = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        close();
      }
    };

    document.addEventListener("pointerdown", handleOutside);
    document.addEventListener("keydown", handleKeyboard);
    return () => {
      document.removeEventListener("pointerdown", handleOutside);
      document.removeEventListener("keydown", handleKeyboard);
    };
  }, [open, close, rootRef, ignoreSelectors]);
};


export default MenuItem;
export {
  MenuItem,
  MenuSub,
  MenuSubTrigger,
  MenuSubContent,
  MenuTrigger,
  MenuContent,
  useDropdownPositioning,
  useMenuIds,
  useMenuCloseHandlers,
};
