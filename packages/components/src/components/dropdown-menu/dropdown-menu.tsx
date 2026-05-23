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
  useDropdownPositioning,
  useMenuIds,
  useMenuCloseHandlers,
} from "../menu";
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
 * Scarlet Ui Dropdown Menu.
 */
const DropdownMenu = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  const [open, setOpenState] = useState(false);
  const [resetNonce, setResetNonce] = useState(0);
  const [positionNonce, setPositionNonce] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLElement | null>(null);

  const { triggerId, contentId } = useMenuIds("dropdown-menu");

  const setOpen = (value: boolean) => {
    if (value) {
      setResetNonce((prev) => prev + 1);
      setPositionNonce((prev) => prev + 1);
    }
    setOpenState(value);
  };

  const close = () => setOpenState(false);

  // Ignore clicks inside any rendered dropdown menu content/portals
  useMenuCloseHandlers(open, close, rootRef, [
    '[data-dropdown-menu-content="true"]',
    '[data-context-menu-content="true"]',
  ]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const handleResize = () => {
      setPositionNonce((prev) => prev + 1);
    };

    const handleScroll = () => {
      setPositionNonce((prev) => prev + 1);
    };

    window.addEventListener("resize", handleResize);
    window.addEventListener("scroll", handleScroll, true);
    return () => {
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

  return (
    <MenuTrigger
      asChild={asChild}
      id={triggerId}
      aria-haspopup="menu"
      aria-expanded={open}
      aria-controls={open ? contentId : undefined}
      data-state={open ? "open" : "closed"}
      className={cn("dropdown_menu_trigger", className)}
      onClick={handleClick}
      {...rest}
    >
      {children}
    </MenuTrigger>
  );
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
  const { open, contentId, triggerId, triggerRef, positionNonce } =
    useDropdownMenu();
  const contentRef = useRef<HTMLDivElement>(null);

  useDropdownPositioning(contentRef, triggerRef, open, positionNonce);

  return (
    <MenuContent
      {...props}
      open={open}
      contentRef={contentRef}
      id={contentId}
      triggerId={triggerId}
      ariaLabel="Dropdown Menu"
      contentDataAttr="dropdown-menu-content"
      className={cn("dropdown_menu_content", props.className)}
      style={props.style}
    />
  );
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

const DropdownMenuItem = (props: DropdownMenuItemProps) => {
  const { close } = useDropdownMenu();
  return <MenuItem {...props} className="dropdown_menu_item" closeMenu={close} />;
};

/**
 * DropdownMenuGroup groups related menu items together visually and semantically.
 */const DropdownMenuGroup = (props: React.HTMLAttributes<HTMLDivElement>) => {
  const { className, children, ...rest } = props;
  return (
    <div className={cn("dropdown_menu_group", className)} role="group" {...rest}>
      {children}
    </div>
  );
};

type DropdownMenuSubProps = React.HTMLAttributes<HTMLDivElement>;

const DropdownMenuSub = (props: DropdownMenuSubProps) => {
  const { resetNonce } = useDropdownMenu();
  return <MenuSub {...props} className="dropdown_menu_sub" resetNonce={resetNonce} />;
};

type DropdownMenuSubTriggerProps = React.ComponentProps<typeof Button>;

/**
 * DropdownMenuSubTrigger acts as a bridge interactive row element that expands
 * adjacent child sub-content popovers upon interaction.
 */
const DropdownMenuSubTrigger = (props: DropdownMenuSubTriggerProps) => (
  <MenuSubTrigger
    {...props}
    className="dropdown_menu_subtrigger"
    arrowClassName="dropdown_menu_subarrow"
  />
);

type DropdownMenuSubContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * DropdownMenuSubContent handles rendering nested secondary flyout card modules
 * adjacent to active sub-triggers.
 */
const DropdownMenuSubContent = (props: DropdownMenuSubContentProps) => (
  <MenuSubContent {...props} className="dropdown_menu_subcontent" />
);

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
