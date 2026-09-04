import TopNavBar from "./menu/top-nav-bar";
import UserMenu from "./user-menu";
import { NavPortalProvider } from "./menu/top-nav-bar/nav-portal-context";
import styles from "./layout.module.css";
import { ThemeSwitcher, type ThemeOption } from "@sun/themes";
import { useEffect, useState } from "react";

type LayoutProps = React.PropsWithChildren;

/**
 * App shell wrapping every page.
 */
const Layout = (props: LayoutProps) => {
  const { children } = props;
  const [themes, setThemes] = useState<ThemeOption[]>([]);

  useEffect(() => {
    setThemes(window.__themes__ ?? []);
  }, []);

  return (
    <NavPortalProvider>
      <main className={styles.main}>
        <TopNavBar />
        <UserMenu />
        {children}
      </main>
      <div className={styles.switcher}>
        <ThemeSwitcher themes={themes} />
      </div>
    </NavPortalProvider>
  );
};

export default Layout;
