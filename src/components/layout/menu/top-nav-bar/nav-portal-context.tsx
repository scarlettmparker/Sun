import { createContext, useContext, useState, type ReactNode } from "react";

type NavPortalContextType = {
  /**
   * Outer slot from layout (e.g. list/create).
   */
  outerSlot: HTMLElement | null;
  /**
   * Inner slot from detail (above breadcrumb).
   */
  innerSlot: HTMLElement | null;
  /**
   * Setter for outer slot.
   */
  setOuterSlot: (el: HTMLElement | null) => void;
  /**
   * Setter for inner slot.
   */
  setInnerSlot: (el: HTMLElement | null) => void;
};

const NavPortalContext = createContext<NavPortalContextType | null>(null);

/**
 * Provides nav portal slots for layout/detail.
 */
export const NavPortalProvider = ({ children }: { children: ReactNode }) => {
  const [outerSlot, setOuterSlot] = useState<HTMLElement | null>(null);
  const [innerSlot, setInnerSlot] = useState<HTMLElement | null>(null);

  return (
    <NavPortalContext.Provider
      value={{ outerSlot, innerSlot, setOuterSlot, setInnerSlot }}
    >
      {children}
    </NavPortalContext.Provider>
  );
};

/**
 * Consumes nav portal context.
 */
export const useNavPortal = (): NavPortalContextType => {
  const ctx = useContext(NavPortalContext);
  if (!ctx)
    throw new Error("useNavPortal must be used within NavPortalProvider");
  return ctx;
};
