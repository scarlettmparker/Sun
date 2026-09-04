import { useTranslation } from "react-i18next";
import {
  Card,
  CardBody,
  CardDescription,
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
    <div className={className} {...rest}>
      <h2 className={styles.section_title}>{t("experience.title")}</h2>
      <Card>
        <CardHeader>
          <CardTitle>{t("experience.certus.company")}</CardTitle>
          <CardDescription>{t("experience.certus.date")}</CardDescription>
        </CardHeader>
        <CardBody>
          <p className={styles.body}>{t("experience.certus.body")}</p>
        </CardBody>
      </Card>
    </div>
  );
};

export default ExperienceSection;
