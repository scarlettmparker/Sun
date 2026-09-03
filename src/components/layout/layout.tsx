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
  const [themes, setThemes] = useState<ThemeOption[]>(() =>
    typeof window !== "undefined"
      ? ((window as unknown as { __themes__?: ThemeOption[] }).__themes__ ?? [])
      : [],
  );
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
    const windowThemes = (window as unknown as { __themes__?: ThemeOption[] })
      .__themes__;
    if (windowThemes?.length) {
      setThemes(windowThemes);
    }
    if (themesData == null) {
      if (!windowThemes?.length) {
        refetchEntry("themes", "themes", {}, forceUpdate);
      }
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
    const getCurrentTheme = (): Record<string, string> | null => {
      if (typeof window === "undefined") return null;
      const stored = window.localStorage.getItem("sun:theme");
      if (stored) {
        try {
          const parsed = JSON.parse(stored) as Record<string, string>;
          if (parsed?.["primary"]) return parsed;
        } catch {
          // ignore
        }
      }
      const winTheme = (window as unknown as { __theme__?: Record<string, string> })
        .__theme__;
      if (winTheme?.["primary"]) return winTheme;
      if (themesData?.current) {
        return themesData.current as unknown as Record<string, string>;
      }
      return null;
    };
    const update = () => setBackgroundColour(getBackgroundHex(getCurrentTheme()));
    update();
    const handleTheme = () => setBackgroundColour(getBackgroundHex(getCurrentTheme()));
    window.addEventListener(THEME_APPLIED_EVENT, handleTheme);
    return () => window.removeEventListener(THEME_APPLIED_EVENT, handleTheme);
  }, [themesData]);

  return (
    <NavPortalProvider>
      <main style={{ backgroundColor: backgroundColour }} className={styles.main}>
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
