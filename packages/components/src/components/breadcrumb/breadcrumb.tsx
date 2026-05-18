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
   * Separator node.
   */
  separator: React.ReactNode;
  /**
   * Current active href.
   */
  current?: string;
  /**
   * Replace all breadcrumbs.
   */
  setBreadcrumbs: (crumbs: Crumb[]) => void;
  /**
   * Append one crumb.
   */
  addBreadcrumb: (crumb: Crumb) => void;
  /**
   * Remove last crumb.
   */
  popBreadcrumb: () => void;
  /**
   * Remove last N crumbs.
   */
  popBreadcrumbs: (count: number) => void;
  /**
   * Delete crumb at index.
   */
  deleteBreadcrumb: (index: number) => void;
  /**
   * Delete multiple by indices.
   */
  deleteBreadcrumbs: (indices: number[]) => void;
  /**
   * Remove all.
   */
  deleteAllBreadcrumbs: () => void;
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

  const addBreadcrumb = useCallback((crumb: Crumb) => {
    setCrumbs((prev) => [...prev, crumb]);
  }, []);

  const popBreadcrumb = useCallback(() => {
    setCrumbs((prev) => prev.slice(0, -1));
  }, []);

  const popBreadcrumbs = useCallback((count: number) => {
    setCrumbs((prev) => prev.slice(0, -Math.max(0, count)));
  }, []);

  const deleteBreadcrumb = useCallback((index: number) => {
    setCrumbs((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const deleteBreadcrumbs = useCallback((indices: number[]) => {
    setCrumbs((prev) => prev.filter((_, i) => !indices.includes(i)));
  }, []);

  const deleteAllBreadcrumbs = useCallback(() => {
    setCrumbs([]);
  }, []);

  const setCurrent = useCallback((href?: string) => {
    setCurrentState(href);
  }, []);

  const contextValue: BreadcrumbContextValue = {
    crumbs,
    separator,
    current,
    setBreadcrumbs,
    addBreadcrumb,
    popBreadcrumb,
    popBreadcrumbs,
    deleteBreadcrumb,
    deleteBreadcrumbs,
    deleteAllBreadcrumbs,
    setCurrent,
  };

  return (
    <BreadcrumbContext.Provider value={contextValue}>
      <nav aria-label="breadcrumb" {...rest}>
        <ol className={cn("breadcrumb", className)}>
          {crumbs.map((crumb, index) => (
            <BreadcrumbItem
              key={index}
              href={crumb.href}
              active={current === crumb.href}
            >
              {crumb.label}
            </BreadcrumbItem>
          ))}
        </ol>
        {children}
      </nav>
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
};

/**
 * Individual accessible breadcrumb item component.
 * Can be used inside Breadcrumb or standalone.
 */
const BreadcrumbItem = (props: BreadcrumbItemProps) => {
  const { href, active, children, className, ...rest } = props;
  const ctx = useContext(BreadcrumbContext);
  const separator = ctx?.separator ?? "/";
  const isActive =
    active ?? (ctx?.current !== undefined && ctx.current === href);

  return (
    <li
      className={cn("breadcrumb-item", isActive && "active", className)}
      aria-current={isActive ? "page" : undefined}
      {...rest}
    >
      {href && !isActive ? <a href={href}>{children}</a> : children}
      {!isActive && <span className="separator">{separator}</span>}
    </li>
  );
};

export default Breadcrumb;
export { BreadcrumbItem, BreadcrumbContext };
export type { BreadcrumbContextValue };
