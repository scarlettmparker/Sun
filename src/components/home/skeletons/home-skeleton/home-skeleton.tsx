import { Card, CardBody, CardHeader, Skeleton } from "@sun/components";
import styles from "./home-skeleton.module.css";

/**
 * Skeleton for home page. Mirrors HeroCard + SpotifyCard + ProjectGrid.
 */
const HomeSkeleton = () => {
  return (
    <div className={styles.home_wrapper}>
      <div className={styles.hero_row}>
        <Card className={styles.hero_card}>
          <CardHeader>
            <div className={styles.hero_header}>
              <Skeleton className={styles.avatar} />
              <div className={styles.hero_text}>
                <Skeleton className={styles.hero_title} />
                <Skeleton className={styles.hero_subtitle} />
              </div>
            </div>
          </CardHeader>
          <CardBody>
            <Skeleton className={styles.hero_bio} />
          </CardBody>
        </Card>
        <Card className={styles.spotify_card}>
          <CardHeader>
            <Skeleton className={styles.spotify_title} />
          </CardHeader>
          <CardBody>
            <Skeleton className={styles.spotify_body} />
          </CardBody>
        </Card>
      </div>
      <div className={styles.grid_section}>
        <Skeleton className={styles.section_title} />
        <div className={styles.grid}>
          {Array.from({ length: 6 }, (_, i) => (
            <Card key={i} className={styles.card}>
              <CardHeader>
                <Skeleton className={styles.card_title} />
              </CardHeader>
              <CardBody>
                <Skeleton className={styles.card_body} />
              </CardBody>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
};

export default HomeSkeleton;
