import { useState } from "react";
import { ScrollArea } from "@sun/components";
import HeroCard from "~/components/home/hero-card";
import SpotifyCard from "~/components/home/spotify-card";
import ProjectGrid from "~/components/home/project-grid";
import ProjectDetailDialog from "~/components/home/project-detail-dialog";
import {
  internalProjects,
  personalProjects,
  type Project,
} from "~/components/home/projects/projects-config";
import styles from "./index.module.css";

/**
 * Home landing page.
 */
const HomePage = () => {
  const [selected, setSelected] = useState<Project | null>(null);

  return (
    <ScrollArea maxHeight="100dvh" className={styles.scroll_area}>
      <div className={styles.home_wrapper}>
        <div className={styles.hero_row}>
          <HeroCard className={styles.hero_card} />
          <SpotifyCard className={styles.spotify_card} />
        </div>
        <ProjectGrid
          titleKey="projects.personal-title"
          projects={[...personalProjects, ...internalProjects]}
          onReadMore={setSelected}
        />
        <ProjectDetailDialog
          project={selected}
          onOpenChange={(open) => !open && setSelected(null)}
        />
      </div>
    </ScrollArea>
  );
};

export default HomePage;
