import { useTranslation } from "react-i18next";
import { Card, CardHeader, CardTitle } from "@sun/components";
import ProjectCard from "../project-card";
import type { Project } from "../projects/projects-config";
import styles from "./project-grid.module.css";

type ProjectGridProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * i18n key for section title.
   */
  titleKey: string;
  /**
   * Projects to render.
   */
  projects: Project[];
  /**
   * Called when a project's read more is clicked.
   */
  onReadMore: (project: Project) => void;
};

/**
 * Grid of project cards for a section.
 */
const ProjectGrid = (props: ProjectGridProps) => {
  const { titleKey, projects, onReadMore, className, ...rest } = props;
  const { t } = useTranslation("home");
  const sorted = [...projects].sort(
    (a, b) => a.rank - b.rank,
  );

  return (
    <div className={className} {...rest}>
      <Card>
        <CardHeader>
          <CardTitle>{t(titleKey)}</CardTitle>
        </CardHeader>
      </Card>
      <div className={styles.grid}>
        {sorted.map((project) => (
          <ProjectCard
            key={project.key}
            project={project}
            onReadMore={onReadMore}
            className={styles.card}
          />
        ))}
      </div>
    </div>
  );
};

export default ProjectGrid;
