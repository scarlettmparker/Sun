import { memo } from "react";
import { useTranslation } from "react-i18next";
import {
  Card,
  CardBody,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@sun/components";
import { cn } from "~/utils/cn";
import styles from "./spotify-card.module.css";

type SpotifyCardProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Square card with cute art and Spotify link, styled like the hero card.
 */
const SpotifyCard = (props: SpotifyCardProps) => {
  const { className, ...rest } = props;
  const { t } = useTranslation("home");

  return (
    <Card className={cn(styles.card, className)} {...rest}>
      <CardHeader>
        <div className={styles.header_row}>
          <div className={styles.header_text}>
            <CardTitle>{t("spotify.title")}</CardTitle>
            <CardDescription>{t("spotify.subtitle")}</CardDescription>
          </div>
          <a
            href="https://open.spotify.com/track/2Vl0e5vhhMrQipEwU2ihpL"
            target="_blank"
            rel="noopener noreferrer"
            className={styles.art_link}
            title={t("spotify.listen")}
            aria-label={t("spotify.listen")}
          >
            <img
              src="/cute.png"
              alt={t("spotify.image-alt")}
              className={styles.art}
              width={64}
              height={64}
            />
          </a>
        </div>
      </CardHeader>
      <CardBody>
        <div className={styles.body}>
          <p className={styles.body_text}>
            {t("spotify.body")}
            {" · "}
            <a
              href="https://open.spotify.com/track/2Vl0e5vhhMrQipEwU2ihpL"
              target="_blank"
              rel="noopener noreferrer"
              className={styles.spotify_link}
              title={t("spotify.listen")}
              aria-label={t("spotify.listen")}
            >
              {t("spotify.listen")}
            </a>
          </p>
        </div>
      </CardBody>
    </Card>
  );
};

export default memo(SpotifyCard);
