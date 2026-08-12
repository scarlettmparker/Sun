import { useTranslation } from "react-i18next";
import AppCard from "~/_components/hub/app-card";
import { useHub } from "~/routes/hub/hub-context";
import styles from "./app-grid.module.css";

/**
 * Grid of hub app cards.
 */
const AppGrid = () => {
  const { t } = useTranslation("hub");
  const { registry, statuses } = useHub();
  const apps = registry?.apps ?? [];

  return (
    <div className={styles.apps_grid}>
      {apps.map((app) => (
        <AppCard key={app.key} app={app} status={statuses[app.key]} />
      ))}
      {!apps.length && <p className={styles.no_apps}>{t("empty")}</p>}
    </div>
  );
};

export default AppGrid;
