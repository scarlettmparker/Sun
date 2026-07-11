import React, {
  cloneElement,
  createContext,
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ReactElement,
} from "react";
import { createPortal } from "react-dom";
import { cn } from "~/utils/cn";
import styles from "./tooltip.module.css";

const useIsomorphicLayoutEffect =
  typeof window !== "undefined" ? useLayoutEffect : useEffect;

const OPEN_DELAY = 400;
const CLOSE_DELAY = 650;

/**
 * Shared state across all tooltips in a group, enabling the instant-switch
 * behaviour where a second tooltip opens with zero delay if one is already visible.
 */
type TooltipGroupContextValue = {
  /**
   * Whether any tooltip in the group is currently open.
   */
  anyOpen: boolean;
  /**
   * Timestamp of the last tooltip close, for the instant-reopen rule.
   */
  lastClosedAt: number;
  /**
   * Called when a tooltip becomes visible.
   */
  onOpen: () => void;
  /**
   * Called when a tooltip is hidden.
   */
  onClose: () => void;
};

const TooltipGroupContext = createContext<TooltipGroupContextValue>({
  anyOpen: false,
  lastClosedAt: 0,
  onOpen: () => {},
  onClose: () => {},
});

/**
 * Context value for a single tooltip's internal state.
 */
type TooltipContextValue = {
  /**
   * Whether this tooltip is currently visible.
   */
  open: boolean;
  /**
   * Skip the fade-in animation (recent tooltip switch).
   */
  instant: boolean;
  /**
   * Mutable ref to the trigger element, used for positioning.
   */
  triggerRef: React.MutableRefObject<HTMLElement | null>;
  /**
   * Start the open timer (respects the group delay rules).
   */
  show: () => void;
  /**
   * Start the close timer.
   */
  hide: () => void;
};

const TooltipContext = createContext<TooltipContextValue | null>(null);

/**
 * Internal hook to access the tooltip context.
 *
 * @throws if used outside a Tooltip
 */
const useTooltip = () => {
  const ctx = useContext(TooltipContext);
  if (!ctx) {
    throw new Error("Tooltip components must be used inside a Tooltip");
  }
  return ctx;
};

/**
 * Hook that positions the content element relative to the trigger, flipping
 * to the opposite side when there is not enough viewport space.
 *
 * @param contentRef ref to the content element to position
 * @param triggerRef ref to the trigger element to position against
 * @param open whether the tooltip is visible
 * @param side the preferred side
 */
const useTooltipPositioning = (
  contentRef: React.RefObject<HTMLDivElement | null>,
  triggerRef: React.MutableRefObject<HTMLElement | null>,
  open: boolean,
  side: "top" | "bottom" | "left" | "right",
) => {
  useIsomorphicLayoutEffect(() => {
    if (!open || !contentRef.current || !triggerRef.current) {
      return;
    }

    const update = () => {
      const content = contentRef.current;
      if (!content || !triggerRef.current) {
        return;
      }

      const trigger = triggerRef.current.getBoundingClientRect();
      const rect = content.getBoundingClientRect();
      const gap = 6;
      let actualSide = side;

      if (side === "top" && trigger.top < rect.height + gap) {
        actualSide = "bottom";
      }
      if (
        side === "bottom" &&
        trigger.bottom + rect.height + gap > window.innerHeight
      ) {
        actualSide = "top";
      }
      if (side === "left" && trigger.left < rect.width + gap) {
        actualSide = "right";
      }
      if (
        side === "right" &&
        trigger.right + rect.width + gap > window.innerWidth
      ) {
        actualSide = "left";
      }

      let top = 0;
      let left = 0;

      switch (actualSide) {
        case "top":
          top = trigger.top - rect.height - gap;
          left = trigger.left + trigger.width / 2 - rect.width / 2;
          break;
        case "bottom":
          top = trigger.bottom + gap;
          left = trigger.left + trigger.width / 2 - rect.width / 2;
          break;
        case "left":
          top = trigger.top + trigger.height / 2 - rect.height / 2;
          left = trigger.left - rect.width - gap;
          break;
        case "right":
          top = trigger.top + trigger.height / 2 - rect.height / 2;
          left = trigger.right + gap;
          break;
      }

      const vw = window.innerWidth;
      const vh = window.innerHeight;
      left = Math.max(gap, Math.min(left, vw - rect.width - gap));
      top = Math.max(gap, Math.min(top, vh - rect.height - gap));

      content.style.top = `${top}px`;
      content.style.left = `${left}px`;
    };

    update();
    let raf: number;
    const loop = () => {
      update();
      raf = requestAnimationFrame(loop);
    };
    loop();
    return () => cancelAnimationFrame(raf);
  }, [open, side, contentRef, triggerRef]);
};

/**
 * Component prop types for TooltipGroup.
 */
type TooltipGroupProps = React.PropsWithChildren;

/**
 * Wraps multiple tooltips so they share open state, enabling instant switching
 * between tooltips without the open delay.
 *
 * @param children the tooltip elements
 */
const TooltipGroup = ({ children }: TooltipGroupProps) => {
  const [anyOpen, setAnyOpen] = useState(false);
  const [lastClosedAt, setLastClosedAt] = useState(0);
  const openCount = useRef(0);

  return (
    <TooltipGroupContext.Provider
      value={{
        anyOpen,
        lastClosedAt,
        onOpen: () => {
          openCount.current++;
          setAnyOpen(true);
        },
        onClose: () => {
          openCount.current = Math.max(0, openCount.current - 1);
          if (openCount.current === 0) {
            setAnyOpen(false);
            setLastClosedAt(Date.now());
          }
        },
      }}
    >
      {children}
    </TooltipGroupContext.Provider>
  );
};

/**
 * Component prop types for Tooltip.
 */
type TooltipProps = React.PropsWithChildren<{
  /**
   * Overrides hover to control visibility directly.
   */
  open?: boolean;
}>;

/**
 * Manages the open/close timers and shared state for a single tooltip.
 */
const Tooltip = ({ children, open: controlledOpen }: TooltipProps) => {
  const [internalOpen, setInternalOpen] = useState(false);
  const [instant, setInstant] = useState(false);
  const isControlled = controlledOpen !== undefined;
  const open = isControlled ? controlledOpen : internalOpen;
  const triggerRef = useRef<HTMLElement | null>(null);
  const group = useContext(TooltipGroupContext);
  const openTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearTimers = () => {
    if (openTimer.current) {
      clearTimeout(openTimer.current);
      openTimer.current = null;
    }
    if (closeTimer.current) {
      clearTimeout(closeTimer.current);
      closeTimer.current = null;
    }
  };

  const show = () => {
    if (isControlled) return;
    clearTimers();
    const delay = group.anyOpen ? 0 : OPEN_DELAY;
    openTimer.current = setTimeout(() => {
      setInstant(Date.now() - group.lastClosedAt < 500);
      setInternalOpen(true);
      group.onOpen();
    }, delay);
  };

  const hide = () => {
    if (isControlled) return;
    clearTimers();
    closeTimer.current = setTimeout(() => {
      setInternalOpen(false);
      group.onClose();
    }, CLOSE_DELAY);
  };

  useEffect(() => () => clearTimers(), []);

  return (
    <TooltipContext.Provider
      value={{
        open,
        instant: isControlled || instant,
        triggerRef,
        show,
        hide,
      }}
    >
      {children}
    </TooltipContext.Provider>
  );
};

/**
 * Component prop types for TooltipTrigger.
 */
type TooltipTriggerProps = {
  children: ReactElement;
  /**
   * Render the child element directly instead of a wrapper span.
   */
  asChild?: boolean;
};

/**
 * Wraps the element that triggers the tooltip on hover or focus.
 *
 * @param children the trigger element
 * @param asChild render the child directly instead of wrapping in a span
 */
const TooltipTrigger = ({ children, asChild }: TooltipTriggerProps) => {
  const { show, hide, triggerRef } = useTooltip();

  const sharedProps = {
    onMouseEnter: show,
    onMouseLeave: hide,
    onFocus: show,
    onBlur: hide,
    ref: (el: HTMLElement | null) => {
      triggerRef.current = el;
    },
  };

  if (asChild && React.isValidElement(children)) {
    const child = children as ReactElement<Record<string, unknown>>;
    const childProps = child.props as Record<string, unknown>;
    return cloneElement(child, {
      ...sharedProps,
      className: childProps.className,
    });
  }

  return <span {...sharedProps}>{children}</span>;
};

/**
 * Component prop types for TooltipContent.
 */
type TooltipContentProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Preferred side of the trigger to render on. Flips if viewport space is insufficient.
   */
  side?: "top" | "bottom" | "left" | "right";
};

/**
 * The floating tooltip content. Portaled to the body with viewport-aware
 * positioning.
 *
 * @param side the preferred side
 * @param className additional class names
 * @param children the tooltip content
 */
const TooltipContent = ({
  side = "top",
  className,
  children,
  ...rest
}: TooltipContentProps) => {
  const { open, instant, triggerRef, show, hide } = useTooltip();
  const contentRef = useRef<HTMLDivElement>(null);

  useTooltipPositioning(contentRef, triggerRef, open, side);

  if (!open) {
    return null;
  }

  return createPortal(
    <div
      ref={contentRef}
      className={cn(styles.content, !instant && styles.fade_in, className)}
      onMouseEnter={show}
      onMouseLeave={hide}
      {...rest}
    >
      {children}
    </div>,
    document.body,
  );
};

export default Tooltip;
export { TooltipGroup, TooltipTrigger, TooltipContent };
