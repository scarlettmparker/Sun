import { memo, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { FileText } from "lucide-react";
import {
  Card,
  CardBody,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@sun/components";
import { getAge, getExperienceYoE } from "~/utils/age";
import styles from "./hero-card.module.css";

type HeroCardProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Intro card with avatar, bio, age and GitHub link.
 */
const HeroCard = (props: HeroCardProps) => {
  const { t } = useTranslation("home");
  const dob = useMemo(() => new Date(2003, 1, 21), []);
  const age = useMemo(() => getAge(dob), [dob]);
  const birthdayLabel = useMemo(() => t("hero.birthday-label"), [t]);
  const start = useMemo(() => new Date(2025, 2, 10), []);
  const yoe = useMemo(() => getExperienceYoE(start), [start]);
  const experience = useMemo(() => t("hero.experience", { count: yoe }), [t, yoe]);
  const experienceLabel = useMemo(() => t("hero.experience-label"), [t]);

  return (
    <Card {...props}>
      <CardHeader>
        <div className={styles.header_row}>
          <a
            href="https://open.spotify.com/track/2vEgk5YyoPKzR9eOIs9SFF"
            target="_blank"
            rel="noopener noreferrer"
            className={styles.avatar_link}
            title={t("hero.listen")}
            aria-label={t("hero.listen")}
          >
            <img
              src="/art.jpg"
              alt="Scarlett Parker"
              className={styles.avatar}
              width={64}
              height={64}
            />
          </a>
          <div className={styles.header_text}>
            <CardTitle className={styles.title_row}>
              <span>{t("hero.name")}</span>
              <a
                href="/cv.pdf"
                target="_blank"
                rel="noopener noreferrer"
                className={styles.cv_link}
                title="Open CV"
                aria-label="Open CV"
              >
                <FileText className={styles.cv_icon} aria-hidden="true" />
                CV
              </a>
            </CardTitle>
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
            {t("hero.bio-prefix")} |{" "}
            <span
              className={styles.age}
              title={experienceLabel}
              aria-label={experienceLabel}
            >
              {experience}
            </span>
            {" | "}
            <span title={birthdayLabel} aria-label={birthdayLabel}>
              {t("hero.age", { count: age })}
            </span>
          </p>
          <p className={styles.bio_detail}>{t("hero.bio-detail")}</p>
        </div>
      </CardBody>
    </Card>
  );
};

export default memo(HeroCard);
