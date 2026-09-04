import { memo, useCallback } from "react";
import { useTranslation } from "react-i18next";
import {
  Badge,
  Button,
  Card,
  CardBody,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@sun/components";
import type { Project } from "../projects/projects-config";
import styles from "./project-card.module.css";

type ProjectCardProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Project to display.
   */
  project: Project;
  /**
   * Called when read more is clicked.
   */
  onReadMore: (project: Project) => void;
};

/**
 * Card for a single project with read-more action.
 */
const ProjectCard = (props: ProjectCardProps) => {
  const { project, onReadMore, className, ...rest } = props;
  const { t } = useTranslation("home");

  const handleClick = useCallback(() => {
    onReadMore(project);
  }, [onReadMore, project]);

  return (
    <Card className={className} {...rest}>
      <CardHeader>
        <CardTitle className={styles.title_row}>
          <span>{project.title}</span>
          {project.disabled ? (
            <Badge className={styles.badge}>{t("projects.you-are-here")}</Badge>
          ) : (
            project.visitHref &&
            project.visitHref !== project.href && (
              <a
                href={project.visitHref}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.visit_link}
              >
                {t("projects.visit")}
              </a>
            )
          )}
        </CardTitle>
        {project.href?.includes("github.com") && (
          <CardDescription>
            <a
              href={project.href}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.github_link}
              title={t("projects.visit-github")}
              aria-label={t("projects.visit-github")}
            >
              {project.href}
            </a>
          </CardDescription>
        )}
      </CardHeader>
      <CardBody>
        <p className={styles.description}>{t(project.descriptionKey)}</p>
      </CardBody>
      <CardFooter className={styles.footer}>
        <Button variant="secondary" aria-label={t("projects.read-more")} onClick={handleClick}>
          {t("projects.read-more")}
        </Button>
      </CardFooter>
    </Card>
  );
};

export default memo(ProjectCard);
