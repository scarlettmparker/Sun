import React, { createContext, useContext, useState, useCallback } from "react";
import { cn } from "~/utils/cn";
import "./breadcrumb.module.css";

export type Crumb = {
  /**
   * Label for the breadcrumb item.
   */
  label: React.ReactNode;
  /**
   * Optional href for link.
   */
  href?: string;
};

/**
 * Value provided by BreadcrumbContext for controlling breadcrumbs dynamically.
 */
type BreadcrumbContextValue = {
  /**
   * Current list of crumbs.
   */
  crumbs: Crumb[];
  /**
   * Current active href.
   */
  current?: string;
  /**
   * Replace all breadcrumbs.
   */
  setBreadcrumbs: (crumbs: Crumb[]) => void;
  /**
   * Set active href.
   */
  setCurrent: (href?: string) => void;
};

const BreadcrumbContext = createContext<BreadcrumbContextValue | null>(null);

type BreadcrumbProps = React.HTMLAttributes<HTMLElement> & {
  /**
   * Custom separator between items, defaults to "/".
   */
  separator?: React.ReactNode;
};

/**
 * Scarlet UI Breadcrumb root. Manages dynamic crumbs list via context.
 * Renders accessible nav/ol with BreadcrumbItem children.
 */
const Breadcrumb = (props: BreadcrumbProps) => {
  const { separator = "/", className, children, ...rest } = props;
  const [crumbs, setCrumbs] = useState<Crumb[]>([]);
  const [current, setCurrentState] = useState<string | undefined>();

  const setBreadcrumbs = useCallback((newCrumbs: Crumb[]) => {
    setCrumbs(newCrumbs);
  }, []);

  const setCurrent = useCallback((href?: string) => {
    setCurrentState(href);
  }, []);

  const contextValue: BreadcrumbContextValue = {
    crumbs,
    current,
    setBreadcrumbs,
    setCurrent,
  };

  return (
    <BreadcrumbContext.Provider value={contextValue}>
      <nav aria-label="breadcrumb" {...rest}>
        <ol className={cn("breadcrumb", className)}>
          {crumbs.map((crumb, index) => {
            // The item is visually active if it matches `current` or is the final node
            const isActive =
              current === crumb.href || index === crumbs.length - 1;
            return (
              <BreadcrumbItem
                key={index}
                href={crumb.href}
                active={isActive}
                separator={separator}
              >
                {crumb.label}
              </BreadcrumbItem>
            );
          })}
        </ol>
      </nav>
      {children}
    </BreadcrumbContext.Provider>
  );
};

type BreadcrumbItemProps = React.LiHTMLAttributes<HTMLLIElement> & {
  /**
   * Href for the breadcrumb link (if not active).
   */
  href?: string;
  /**
   * Force active state.
   */
  active?: boolean;
  /**
   * Separator inherited or overridden for the item.
   */
  separator?: React.ReactNode;
};

/**
 * Individual accessible breadcrumb item component.
 * Can be used inside Breadcrumb or standalone.
 */
const BreadcrumbItem = (props: BreadcrumbItemProps) => {
  const { href, active, children, className, separator = "/", ...rest } = props;

  const renderContent = () => (
    <li
      className={cn("breadcrumb_item", active && "active", className)}
      aria-current={active ? "page" : undefined}
      {...rest}
    >
      {children}
    </li>
  );

  return (
    <>
      {href && !active ? <a href={href}>{renderContent()}</a> : renderContent()}
      {!active && <span className="breadcrumb_separator">{separator}</span>}
    </>
  );
};

/**
 * Custom hook to safely consume the Scarlet UI Breadcrumb context.
 */
const useBreadcrumbContext = () => {
  const context = useContext(BreadcrumbContext);
  if (!context) {
    throw new Error(
      "useBreadcrumbContext must be used within a <Breadcrumb /> component",
    );
  }
  return context;
};

export default Breadcrumb;
export { BreadcrumbItem, useBreadcrumbContext };
