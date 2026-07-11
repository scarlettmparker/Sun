import React, {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { cn } from "~/utils/cn";
import Button from "../button";
import {
  MenuItem,
  MenuSub,
  MenuSubTrigger,
  MenuSubContent,
  MenuTrigger,
  MenuContent,
  useMenuIds,
  useMenuCloseHandlers,
} from "../menu";
import styles from "./context-menu.module.css";

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
  const { triggerId, contentId } = useMenuIds("context-menu");

  const setOpen = (value: boolean) => {
    if (value) {
      lastWindowSize.current = { w: window.innerWidth, h: window.innerHeight };
      // Force nested submenus to drop open states when a fresh menu anchor triggers
      setResetNonce((prev) => prev + 1);
    }
    setOpenState(value);
  };

  const close = () => setOpenState(false);

  // Ignore clicks inside any rendered context menu or dropdown menu content/portals
  useMenuCloseHandlers(open, close, rootRef, [
    '[data-context-menu-content="true"]',
    '[data-dropdown-menu-content="true"]',
  ]);

  useEffect(() => {
    if (!open) {
      return;
    }

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

    window.addEventListener("resize", handleResize);
    return () => {
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
        className={cn(styles.context_menu, className)}
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
   * Render the child element directly.
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

  return (
    <MenuTrigger
      asChild={asChild}
      id={triggerId}
      aria-haspopup="menu"
      aria-expanded={open}
      aria-controls={open ? contentId : undefined}
      data-state={open ? "open" : "closed"}
      className={cn(styles.context_menu_trigger, className)}
      onContextMenu={handleContextMenu}
      onClick={handleClick}
      {...rest}
    >
      {children}
    </MenuTrigger>
  );
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
  const { open, contentId, triggerId, position } = useContextMenu();
  const contentRef = useRef<HTMLDivElement>(null);

  return (
    <MenuContent
      {...props}
      open={open}
      contentRef={contentRef}
      id={contentId}
      triggerId={triggerId}
      aria-label="Context Menu"
      contentDataAttr="context-menu-content"
      className={cn(styles.context_menu_content, props.className)}
      style={{
        position: "fixed",
        top: `${position.y}px`,
        left: `${position.x}px`,
        ...props.style,
      }}
    />
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
 * ContextMenuItem renders a selectable entry that closes the context menu
 * after invoking any onSelect handler.
 */
const ContextMenuItem = (props: ContextMenuItemProps) => {
  const { close } = useContextMenu();
  return (
    <MenuItem
      {...props}
      className={cn(styles.context_menu_item, props.className)}
      closeMenu={close}
    />
  );
};

/**
 * ContextMenuGroup groups related menu items together visually and semantically.
 */
const ContextMenuGroup = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  return (
    <div className={cn(styles.context_menu_group, className)} role="group" {...rest}>
      {children}
    </div>
  );
};

type ContextMenuSubProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * ContextMenuSub provides an isolated container for nested submenu items,
 * resetting open state when the parent context menu re-triggers.
 */
const ContextMenuSub = (props: ContextMenuSubProps) => {
  const { resetNonce } = useContextMenu();
  return (
    <MenuSub
      {...props}
      className={cn(styles.context_menu_sub, props.className)}
      resetNonce={resetNonce}
    />
  );
};

type ContextMenuSubTriggerProps = React.ComponentProps<typeof Button>;

/**
 * ContextMenuSubTrigger acts as a bridge interactive row element that expands
 * adjacent child sub-content popovers upon interaction.
 */
const ContextMenuSubTrigger = (props: ContextMenuSubTriggerProps) => (
  <MenuSubTrigger
    {...props}
    className={cn(styles.context_menu_subtrigger, props.className)}
    arrowClassName={styles.context_menu_subarrow}   />
);

type ContextMenuSubContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * ContextMenuSubContent renders the portal-positioned content panel for a
 * nested context submenu when its trigger is active.
 */
const ContextMenuSubContent = (props: ContextMenuSubContentProps) => (
  <MenuSubContent
    {...props}
    className={cn(styles.context_menu_subcontent, props.className)}
  />
);

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
