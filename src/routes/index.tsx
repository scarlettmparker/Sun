import { useState } from "react";
import HeroCard from "~/components/home/hero-card";
import ExperienceSection from "~/components/home/experience-section";
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
    <div className={styles.home_wrapper}>
      <HeroCard />
      <ExperienceSection />
      <ProjectGrid
        titleKey="projects.personal-title"
        projects={personalProjects}
        onReadMore={setSelected}
      />
      <ProjectGrid
        titleKey="projects.internal-title"
        projects={internalProjects}
        onReadMore={setSelected}
      />
      <ProjectDetailDialog
        project={selected}
        onOpenChange={(open) => !open && setSelected(null)}
      />
    </div>
  );
};

export default HomePage;
