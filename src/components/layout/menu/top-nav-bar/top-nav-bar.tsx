import { Suspense } from "react";
import { Link, useLocation } from "react-router-dom";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Button } from "@sun/components";
import { RoleCheck } from "@sun/ssr/react";
import { Cog6ToothIcon } from "@heroicons/react/24/outline";
import { cn } from "~/utils/cn";
import { useNavPortal } from "./nav-portal-context";
import styles from "./top-nav-bar.module.css";

const NAV_ITEMS = [
  { labelKey: "home", href: "/", role: null },
  { labelKey: "blog", href: "/blog", role: null },
  // TODO: sort out permissions
  { labelKey: "hub", href: "/hub", role: "Super Admin" },
  { labelKey: "gallery", href: "/gallery", role: "Super Admin" },
  { labelKey: "stem-player", href: "/stem-player", role: "Super Admin" },
] as const;

const PUBLIC_PATHS = ["/login"];

/**
 * When on any blog page, portals into #blog-detail-nav-slot inside the detail wrapper.
 */
const TopNavBar = () => {
  const { t } = useTranslation("nav");
  const { pathname } = useLocation();
  const { outerSlot, innerSlot } = useNavPortal();
  const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p));
  const isBlog = pathname.startsWith("/blog");
  const portalTarget = innerSlot ?? outerSlot;

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
        const link = (
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
        if (!item.role) {
          return link;
        }
        return (
          <Suspense key={item.href} fallback={null}>
            <RoleCheck roles={[item.role]}>{link}</RoleCheck>
          </Suspense>
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
