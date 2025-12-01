import Button from "~/components/button";
import menu from "..";
import styles from "./top-nav-bar.module.css";
// import { useTranslation } from "react-i18next";
import React from "react";

/**
 * Top Nav Bar component that displays on all pages.
 */
const TopNavBar = () => {
  // const { t } = useTranslation("home");
  const entries = Object.entries(menu);

  return (
    <nav className={styles.top_nav_bar}>
      {entries.map(([key, item], idx) => (
        <React.Fragment key={idx}>
          <a href={item.href ?? `/${key}`}>
            <Button variant="secondary">{item.name}</Button>
          </a>

          {idx < entries.length - 1 && (
            <span className={styles.divider} role="separator" />
          )}
        </React.Fragment>
      ))}
    </nav>
  );
};

export default TopNavBar;
