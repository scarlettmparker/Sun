import React, { cloneElement, ReactElement } from "react";
import { cn } from "~/utils/cn";
import Button from "../button";

/**
 * Props for the reusable MenuItem primitive.
 * Used by both ContextMenuItem and DropdownMenuItem.
 */
export type MenuItemProps = React.ComponentProps<typeof Button> & {
  /**
   * Render the child element instead of the internal button.
   */
  asChild?: boolean;
  /**
   * Callback after the item is selected.
   */
  onSelect?: () => void;
  /**
   * The base CSS module class name to apply (e.g. "context_menu_item" or "dropdown_menu_item").
   * This keeps the emitted DOM classes identical to the previous per-component versions.
   */
  baseClassName: string;
  /**
   * Function provided by the parent menu to close the menu when the item is activated.
   */
  closeMenu: () => void;
};

/**
 * Shared MenuItem implementation containing the action handling,
 * keyboard activation, disabled state, and complex asChild support.
 *
 * This is the single source of truth for item behavior so that
 * ContextMenu and DropdownMenu (and their sub-menus) do not duplicate logic.
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
    baseClassName,
    closeMenu,
    ...rest
  } = props;

  /**
   * Handles click on the item: calls user onClick, onSelect, then closes the menu.
   */
  const handleAction = (event: React.MouseEvent<HTMLButtonElement>) => {
    if (disabled) {
      return;
    }
    onClick?.(event);
    onSelect?.();
    closeMenu();
  };

  /**
   * Handles keyboard activation (Enter/Space) on the item.
   */
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

    /**
     * Clones the asChild element, injecting menu item semantics,
     * proper role/tabIndex, and merged handlers that still call the child's original handlers.
     */
    return cloneElement(child, {
      ...rest,
      type: (childProps.type as string) ?? "button",
      role: (childProps.role as string) ?? "menuitem",
      tabIndex: -1,
      "aria-disabled": (childProps.disabled as boolean) ?? disabled,
      className: cn(
        baseClassName,
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
      className={cn(baseClassName, className)}
      onClick={handleAction}
      onKeyDown={handleKey}
    >
      {children}
    </Button>
  );
};

export default MenuItem;
