import { Suspense, useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Button } from "@sun/components";
import { RoleCheck } from "@sun/ssr/react";
import { Cog6ToothIcon } from "@heroicons/react/24/outline";
import { cn } from "~/utils/cn";
import styles from "./top-nav-bar.module.css";

const NAV_ITEMS = [
  { labelKey: "home", href: "/" },
  { labelKey: "blog", href: "/blog" },
  { labelKey: "gallery", href: "/gallery" },
  { labelKey: "stem-player", href: "/stem-player" },
] as const;

const PUBLIC_PATHS = ["/login"];

/**
 * Top navigation: page links on the left.
 * When on any blog page, portals into #blog-detail-nav-slot inside the detail wrapper.
 */
const TopNavBar = () => {
  const { t } = useTranslation("nav");
  const { pathname } = useLocation();
  const [portalTarget, setPortalTarget] = useState<HTMLElement | null>(null);
  const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p));
  const isBlog = pathname.startsWith("/blog");

  useEffect(() => {
    if (!isBlog) {
      setPortalTarget(null);
      return;
    }
    const els = document.querySelectorAll<HTMLElement>("#blog-detail-nav-slot");
    setPortalTarget(els.length ? (els[els.length - 1] as HTMLElement) : null);
  }, [isBlog, pathname]);

  if (isPublic) {
    return null;
  }

  const nav = (
    <nav
      id="top-nav-bar"
      ref={(el) => {
        if (el) el.setAttribute("data-nav-ref", "top-nav-bar");
      }}
      className={cn(
        styles.top_nav_bar,
        isBlog && portalTarget && styles.top_nav_bar_portalled,
      )}
    >
      {NAV_ITEMS.map((item) => {
        const active =
          item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
        return (
          <Link
            key={item.href}
            to={item.href}
            style={{ textDecoration: "none" }}
          >
            <Button variant={active ? "default" : "secondary"}>
              {t(item.labelKey)}
            </Button>
          </Link>
        );
      })}
      <Suspense fallback={null}>
        <RoleCheck roles={["Admin", "Super Admin"]} match="any">
          <Link to="/admin" style={{ textDecoration: "none" }}>
            <Button
              variant="secondary"
              title={t("admin")}
              aria-label={t("admin")}
            >
              <Cog6ToothIcon width={20} height={20} />
            </Button>
          </Link>
        </RoleCheck>
      </Suspense>
    </nav>
  );

  if (isBlog && portalTarget) {
    return createPortal(nav, portalTarget);
  }

  return nav;
};

export default TopNavBar;
