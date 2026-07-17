import React, {
  createContext,
  useContext,
  useId,
  useState,
} from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "~/utils/cn";
import styles from "./accordion.module.css";

type AccordionContextValue = {
  /**
   * Whether the accordion content is currently shown.
   */
  open: boolean;
  /**
   * Updates the open state.
   */
  setOpen: (open: boolean) => void;
  /**
   * The unique id of the content element, for ARIA wiring.
   */
  contentId: string;
};

const AccordionContext = createContext<AccordionContextValue | null>(null);

const useAccordion = () => {
  const ctx = useContext(AccordionContext);
  if (!ctx) {
    throw new Error("Accordion components must be used inside an Accordion");
  }
  return ctx;
};

type AccordionProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Whether the accordion is open (controlled).
   */
  open?: boolean;
  /**
   * Whether the accordion starts open when uncontrolled.
   */
  defaultOpen?: boolean;
  /**
   * Called when the open state changes.
   */
  onOpenChange?: (open: boolean) => void;
};

/**
 * A single open/close disclosure. Each Accordion manages its own state, so
 * several can be expanded at once.
 */
const Accordion = ({
  open,
  defaultOpen,
  onOpenChange,
  className,
  children,
  ...rest
}: AccordionProps) => {
  const [internalOpen, setInternalOpen] = useState(defaultOpen ?? false);
  const controlled = open !== undefined;
  const isOpen = controlled ? open : internalOpen;
  const contentId = useId();
  const setOpen = (next: boolean) => {
    if (!controlled) {
      setInternalOpen(next);
    }
    onOpenChange?.(next);
  };
  return (
    <AccordionContext.Provider value={{ open: isOpen, setOpen, contentId }}>
      <div className={cn(styles.accordion, className)} {...rest}>
        {children}
      </div>
    </AccordionContext.Provider>
  );
};

type AccordionTriggerProps = React.HTMLAttributes<HTMLButtonElement>;

/**
 * The clickable header that toggles the adjacent AccordionContent.
 */
const AccordionTrigger = ({
  className,
  children,
  onClick,
  ...rest
}: AccordionTriggerProps) => {
  const { open, setOpen, contentId } = useAccordion();
  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setOpen(!open);
    onClick?.(event);
  };
  return (
    <button
      type="button"
      aria-expanded={open}
      aria-controls={contentId}
      className={cn(styles.trigger, className)}
      onClick={handleClick}
      {...rest}
    >
      <span className={styles.trigger_label}>{children}</span>
      <ChevronDown
        className={cn(styles.chevron, open && styles.chevron_open)}
        width={16}
        height={16}
        aria-hidden
      />
    </button>
  );
};

type AccordionContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * The collapsible region. Renders only while open.
 */
const AccordionContent = ({
  className,
  children,
  ...rest
}: AccordionContentProps) => {
  const { open, contentId } = useAccordion();
  if (!open) {
    return null;
  }
  return (
    <div id={contentId} className={cn(styles.content, className)} {...rest}>
      {children}
    </div>
  );
};

export default Accordion;
export { AccordionTrigger, AccordionContent };
