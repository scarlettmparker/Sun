import { createContext, useContext, useEffect, useState } from "react";
import { cn } from "~/utils/cn";
import styles from "./cookie-banner.module.css";

type CookieBannerContextValue = {
  /**
   * Stores the consent decision and hides the banner.
   */
  accept: () => void;
  /**
   * Stores the decline decision and hides the banner.
   */
  decline: () => void;
};

/**
 * Consent actions for the nearest banner.
 */
export const CookieBannerContext =
  createContext<CookieBannerContextValue | null>(null);

const COOKIE_LIFETIME_SECONDS = 60 * 60 * 24 * 365;

/**
 * Reads a named cookie in the browser.
 */
function getCookie(name: string): string | null {
  if (typeof document === "undefined") {
    return null;
  }
  for (const part of document.cookie.split(/;\s*/)) {
    const index = part.indexOf("=");
    if (index < 0) continue;
    if (part.slice(0, index).trim() === name) {
      return decodeURIComponent(part.slice(index + 1));
    }
  }
  return null;
}

/**
 * Stores the consent decision as a client-readable cookie.
 */
function setConsentCookie(name: string, value: string): void {
  document.cookie = `${name}=${encodeURIComponent(value)}; Path=/; SameSite=Lax; Max-Age=${COOKIE_LIFETIME_SECONDS}`;
}

type CookieBannerProps = React.HTMLAttributes<HTMLElement> & {
  /**
   * Name of the consent cookie to read and write.
   */
  cookieName?: string;
};

/**
 * Fixed consent banner, hidden once a decision is stored in a cookie. Compose
 * content with the sub-components and wire actions via useCookieBanner.
 */
const CookieBanner = ({
  cookieName = "cookie_consent",
  className,
  children,
  ...rest
}: CookieBannerProps) => {
  const [decision, setDecision] = useState<string | null>(null);

  useEffect(() => {
    setDecision(getCookie(cookieName));
  }, [cookieName]);

  if (decision) {
    return null;
  }

  const accept = () => {
    setConsentCookie(cookieName, "accepted");
    setDecision("accepted");
  };

  const decline = () => {
    setConsentCookie(cookieName, "declined");
    setDecision("declined");
  };

  return (
    <CookieBannerContext.Provider value={{ accept, decline }}>
      <aside {...rest} className={cn(styles.banner, className)} role="region">
        {children}
      </aside>
    </CookieBannerContext.Provider>
  );
};

type CookieBannerContentProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Groups the banner's text content.
 */
const CookieBannerContent = ({
  className,
  children,
  ...rest
}: CookieBannerContentProps) => {
  return (
    <div {...rest} className={cn(styles.content, className)}>
      {children}
    </div>
  );
};

type CookieBannerTitleProps = React.HTMLAttributes<HTMLElement>;

/**
 * Banner title.
 */
const CookieBannerTitle = ({
  className,
  children,
  ...rest
}: CookieBannerTitleProps) => {
  return (
    <strong {...rest} className={cn(styles.title, className)}>
      {children}
    </strong>
  );
};

type CookieBannerDescriptionProps = React.HTMLAttributes<HTMLParagraphElement>;

/**
 * Explains why the app stores cookies.
 */
const CookieBannerDescription = ({
  className,
  children,
  ...rest
}: CookieBannerDescriptionProps) => {
  return (
    <p {...rest} className={cn(styles.description, className)}>
      {children}
    </p>
  );
};

type CookieBannerActionsProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Groups the banner's action buttons.
 */
const CookieBannerActions = ({
  className,
  children,
  ...rest
}: CookieBannerActionsProps) => {
  return (
    <div {...rest} className={cn(styles.actions, className)}>
      {children}
    </div>
  );
};

/**
 * Hook for the consent actions of the nearest banner.
 */
const useCookieBanner = () => {
  const context = useContext(CookieBannerContext);
  if (!context) {
    throw new Error("useCookieBanner must be used within a <CookieBanner>");
  }
  return context;
};

export default CookieBanner;
export {
  CookieBannerContent,
  CookieBannerTitle,
  CookieBannerDescription,
  CookieBannerActions,
  useCookieBanner,
};
