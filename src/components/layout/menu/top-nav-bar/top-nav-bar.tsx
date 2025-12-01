import Button from "~/components/button";
import menu from "..";
import styles from "./top-nav-bar.module.css";

/**
 * Top Nav Bar component that displays on all pages.
 */
const TopNavBar = () => {
  const entries = Object.entries(menu);

  return (
    <nav className={styles.top_nav_bar}>
      {entries.map(([key, item], idx) => (
        <>
          <a href={item.href ?? `/${key}`}>
            <Button variant="secondary" key={idx}>
              {item.name}
            </Button>
          </a>

          {idx < entries.length - 1 && (
            <span className={styles.divider} role="separator" />
          )}
        </>
      ))}
    </nav>
  );
};

export default TopNavBar;
