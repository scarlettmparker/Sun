import { useTranslation } from "react-i18next";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@sun/components";
import styles from "./experience-section.module.css";

type ExperienceSectionProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Experience section with company history.
 */
const ExperienceSection = (props: ExperienceSectionProps) => {
  const { className, ...rest } = props;
  const { t } = useTranslation("home");

  return (
    <Card className={className} {...rest}>
      <CardHeader>
        <CardTitle>{t("experience.title")}</CardTitle>
      </CardHeader>
      <CardBody>
        <div className={styles.list}>
          <div className={styles.item}>
            <div className={styles.item_header}>
              <span className={styles.company}>
                {t("experience.certus.company")}
              </span>
              <span className={styles.date}>
                {t("experience.certus.date")}
              </span>
            </div>
            <p className={styles.body}>{t("experience.certus.body")}</p>
          </div>
        </div>
      </CardBody>
    </Card>
  );
};

export default ExperienceSection;
