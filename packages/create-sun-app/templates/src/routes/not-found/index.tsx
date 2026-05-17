import { useTranslation } from "react-i18next";
import styles from "./not-found.module.css";

/**
 * Not found page.
 */
const NotFound = () => {
  const { t } = useTranslation("not-found");

  return (
    <main className={styles.not_found}>
      <h1>{t("not-found")}</h1>
      <p>{t("description")}</p>
    </main>
  );
};

export default NotFound;
