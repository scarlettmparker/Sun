import { Suspense } from "react";
import { Link, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "@sun/components";
import { RoleCheck } from "@sun/ssr/react";
import { Cog6ToothIcon } from "@heroicons/react/24/outline";
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
 */
const TopNavBar = () => {
  const { t } = useTranslation("nav");
  const { pathname } = useLocation();
  const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p));
  if (isPublic) {
    return null;
  }

  return (
    <nav className={styles.top_nav_bar}>
      {NAV_ITEMS.map((item) => {
        const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
        return (
          <Link key={item.href} to={item.href} style={{ textDecoration: "none" }}>
            <Button variant={active ? "default" : "secondary"}>{t(item.labelKey)}</Button>
          </Link>
        );
      })}
      <Suspense fallback={null}>
        <RoleCheck roles={["Admin", "Super Admin"]} match="any">
          <Link to="/admin" style={{ textDecoration: "none" }}>
            <Button variant="secondary" title={t("admin")} aria-label={t("admin")}>
              <Cog6ToothIcon width={20} height={20} />
            </Button>
          </Link>
        </RoleCheck>
      </Suspense>
    </nav>
  );
};

export default TopNavBar;
