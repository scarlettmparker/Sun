import TopNavBar from "./menu/top-nav-bar";
import UserMenu from "./user-menu";
import { NavPortalProvider } from "./menu/top-nav-bar/nav-portal-context";
import styles from "./layout.module.css";
import { getBackgroundHex } from "@sun/utils/background-colour";
import {
  ThemeSwitcher,
  THEME_APPLIED_EVENT,
  type ThemeOption,
} from "@sun/themes";
import { applyTheme } from "@sun/themes";
import { cn } from "@sun/utils/cn";
import { useEffect, useLayoutEffect, useReducer, useState } from "react";
import {
  makeCacheKey,
  peekPageData,
  refetchEntry,
  subscribeDataInvalidation,
} from "@sun/ssr";
import type { ResolvedTheme } from "@sun/utils/property-set";

type LayoutProps = React.PropsWithChildren;

const useIsomorphicLayoutEffect =
  typeof window !== "undefined" ? useLayoutEffect : useEffect;

/**
 * App shell wrapping every page.
 */
const Layout = (props: LayoutProps) => {
  const { children } = props;
  const [backgroundColour, setBackgroundColour] = useState<string>(() =>
    getBackgroundHex(),
  );
  const [ready, setReady] = useState(false);
  const [themes, setThemes] = useState<ThemeOption[]>([]);
  const cacheKey = makeCacheKey("themes:themes", {});
  const [, forceUpdate] = useReducer((x: number) => x + 1, 0);

  useEffect(() => {
    return subscribeDataInvalidation((affected) => {
      if (!affected || affected.includes(cacheKey)) {
        forceUpdate();
      }
    });
  }, [cacheKey]);

  const themesData = peekPageData<ResolvedTheme>("themes", "themes", {}) as
    | ResolvedTheme
    | null;

  useEffect(() => {
    if (themesData == null) {
      refetchEntry("themes", "themes", {}, forceUpdate);
    } else {
      setThemes(themesData.all as ThemeOption[]);
      if (
        themesData.current &&
        !window.localStorage.getItem("sun:theme") &&
        !(window as unknown as { __theme__?: unknown }).__theme__
      ) {
        applyTheme(themesData.current as never);
      }
    }
  }, [themesData]);

  useIsomorphicLayoutEffect(() => {
    const update = () => setBackgroundColour(getBackgroundHex());
    update();
    const frame = requestAnimationFrame(() => setReady(true));
    const interval = setInterval(update, 5000);
    window.addEventListener(THEME_APPLIED_EVENT, update);
    return () => {
      cancelAnimationFrame(frame);
      clearInterval(interval);
      window.removeEventListener(THEME_APPLIED_EVENT, update);
    };
  }, []);

  return (
    <NavPortalProvider>
      <main
        style={{ backgroundColor: backgroundColour }}
        className={cn(styles.main, ready && styles.main_ready)}
      >
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
