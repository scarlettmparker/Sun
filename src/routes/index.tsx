import { useTranslation } from "react-i18next";
import { Card, CardBody, CardHeader, CardTitle } from "@sun/components";
import styles from "./index.module.css";

/**
 * Home landing page.
 */
const HomePage = () => {
  const { t } = useTranslation("home");

  return (
    <div className={styles.home_wrapper}>
      <Card>
        <CardHeader>
          <CardTitle>{t("title")}</CardTitle>
        </CardHeader>
        <CardBody>
          <div className={styles.list_body}>
            <p className={styles.welcome}>{t("welcome")}</p>
          </div>
        </CardBody>
      </Card>
    </div>
  );
};

export default HomePage;
