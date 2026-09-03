import { useTranslation } from "react-i18next";
import {
  Card,
  CardBody,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@sun/components";
import { getAge } from "~/utils/age";
import styles from "./hero-card.module.css";

type HeroCardProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Intro card with avatar, bio, age and GitHub link.
 */
const HeroCard = (props: HeroCardProps) => {
  const { t } = useTranslation("home");
  const dob = new Date(2003, 1, 21);
  const age = getAge(dob);
  const birthdayLabel = t("hero.birthday-label");

  return (
    <Card {...props}>
      <CardHeader>
        <div className={styles.header_row}>
          <img
            src="/art.jpg"
            alt="Scarlett Parker"
            className={styles.avatar}
            width={96}
            height={96}
          />
          <div className={styles.header_text}>
            <CardTitle>{t("hero.name")}</CardTitle>
            <CardDescription>
              <span>{t("hero.location")}</span>
              {" · "}
              <a
                href="https://github.com/scarlettmparker"
                target="_blank"
                rel="noopener noreferrer"
                className={styles.github_link}
              >
                {t("hero.github-label")}
              </a>
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardBody>
        <div className={styles.body}>
          <p className={styles.bio}>
            {t("hero.bio-prefix")}{" "}
            <span
              className={styles.age}
              title={birthdayLabel}
              aria-label={birthdayLabel}
            >
              {t("hero.age", { count: age })}
            </span>
            {t("hero.bio-suffix")}
          </p>
          <p className={styles.bio_detail}>{t("hero.bio-detail")}</p>
        </div>
      </CardBody>
    </Card>
  );
};

export default HeroCard;
