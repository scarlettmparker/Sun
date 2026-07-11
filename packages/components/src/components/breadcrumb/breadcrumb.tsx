import React, { createContext, useContext, useState, useCallback } from "react";
import { cn } from "~/utils/cn";
import styles from "./breadcrumb.module.css";

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
  /**
   * Append a new breadcrumb to the list.
   */
  addBreadcrumb: (crumb: Crumb) => void;
  /**
   * Remove the last breadcrumb.
   */
  popBreadcrumb: () => void;
  /**
   * Delete a breadcrumb at the given index.
   */
  deleteBreadcrumb: (index: number) => void;
  /**
   * Remove all breadcrumbs.
   */
  deleteAllBreadcrumbs: () => void;
};

export const BreadcrumbContext = createContext<BreadcrumbContextValue | null>(
  null,
);

type BreadcrumbProviderProps = React.PropsWithChildren;

/**
 * Provides breadcrumb state via context without rendering the nav.
 *
 * @param children The content to render within the provider.
 */
const BreadcrumbProvider = ({ children }: BreadcrumbProviderProps) => {
  const [crumbs, setCrumbs] = useState<Crumb[]>([]);
  const [current, setCurrentState] = useState<string | undefined>();

  const setBreadcrumbs = useCallback((newCrumbs: Crumb[]) => {
    setCrumbs(newCrumbs);
  }, []);

  const setCurrent = useCallback((href?: string) => {
    setCurrentState(href);
  }, []);

  const addBreadcrumb = useCallback((crumb: Crumb) => {
    setCrumbs((prev) => [...prev, crumb]);
  }, []);

  const popBreadcrumb = useCallback(() => {
    setCrumbs((prev) => prev.slice(0, -1));
  }, []);

  const deleteBreadcrumb = useCallback((index: number) => {
    setCrumbs((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const deleteAllBreadcrumbs = useCallback(() => {
    setCrumbs([]);
  }, []);

  const contextValue: BreadcrumbContextValue = {
    crumbs,
    current,
    setBreadcrumbs,
    setCurrent,
    addBreadcrumb,
    popBreadcrumb,
    deleteBreadcrumb,
    deleteAllBreadcrumbs,
  };

  return (
    <BreadcrumbContext.Provider value={contextValue}>
      {children}
    </BreadcrumbContext.Provider>
  );
};

type BreadcrumbProps = React.HTMLAttributes<HTMLElement> & {
  /**
   * Custom separator between items, defaults to "/".
   */
  separator?: React.ReactNode;
};

/**
 * Renders the breadcrumb nav from the nearest BreadcrumbContext. Must be used
 * inside a <BreadcrumbProvider>.
 *
 * @param separator Custom separator between items, defaults to "/".
 */
const Breadcrumb = (props: BreadcrumbProps) => {
  const { separator = "/", className, children, ...rest } = props;
  const context = useContext(BreadcrumbContext);

  if (!context) {
    throw new Error(
      "<Breadcrumb /> must be used within a <BreadcrumbProvider>",
    );
  }

  const { crumbs, current } = context;

  return (
    <nav aria-label={styles.breadcrumb} {...rest}>
      <ol className={cn(styles.breadcrumb, className)}>
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
      {children}
    </nav>
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
      className={cn(styles.breadcrumb_item, active && styles.active, className)}
      aria-current={active ? "page" : undefined}
      {...rest}
    >
      {href && !active ? <a href={href}>{children}</a> : children}
    </li>
  );

  return (
    <>
      {renderContent()}
      {!active && <span className={styles.breadcrumb_separator}>{separator}</span>}
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
      "useBreadcrumbContext must be used within a <BreadcrumbProvider>",
    );
  }
  return context;
};

export default Breadcrumb;
export { BreadcrumbItem, BreadcrumbProvider, useBreadcrumbContext };
