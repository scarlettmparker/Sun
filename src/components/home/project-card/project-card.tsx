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

  return (
    <Card className={className} {...rest}>
      <CardHeader>
        <CardTitle className={styles.title_row}>
          <span>{project.title}</span>
          {project.disabled && (
            <Badge className={styles.badge}>
              {t("projects.you-are-here")}
            </Badge>
          )}
        </CardTitle>
        <CardDescription>
          <span>{t(project.descriptionKey)}</span>
          {project.href && !project.disabled && (
            <>
              {" · "}
              <a
                href={project.href}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.github_link}
              >
                {t("projects.visit")}
              </a>
            </>
          )}
        </CardDescription>
      </CardHeader>
      <CardBody>
        <div className={styles.meta} />
      </CardBody>
      <CardFooter className={styles.footer}>
        <Button
          variant="secondary"
          title={t("projects.read-more")}
          aria-label={t("projects.read-more")}
          onClick={() => onReadMore(project)}
        >
          {t("projects.read-more")}
        </Button>
      </CardFooter>
    </Card>
  );
};

export default ProjectCard;
