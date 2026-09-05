import { memo, useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  Badge,
  Button,
  Dialog,
  DialogBody,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  MarkdownViewer,
  ScrollArea,
} from "@sun/components";
import type { Project } from "../projects/projects-config";
import styles from "./project-detail-dialog.module.css";

type ProjectDetailDialogProps = {
  /**
   * Selected project or null when closed.
   */
  project: Project | null;
  /**
   * Called when open state changes.
   */
  onOpenChange: (open: boolean) => void;
};

/**
 * Dialog showing markdown detail for a project.
 */
const ProjectDetailDialog = (props: ProjectDetailDialogProps) => {
  const { project, onOpenChange } = props;
  const { t } = useTranslation("home");

  const open = project != null;
  const detailKey = project?.detailKey ?? "";
  const markdown = useMemo(
    () => (detailKey ? t(detailKey) : ""),
    [t, detailKey],
  );

  const handleClose = useCallback(() => {
    onOpenChange(false);
  }, [onOpenChange]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange} className={styles.dialog}>
      <DialogHeader>
        <DialogTitle className={styles.title_row}>
          <span>{project?.title ?? ""}</span>
          {project?.disabled ? (
            <Badge className={styles.badge}>{t("projects.you-are-here")}</Badge>
          ) : (
            project?.visitHref &&
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
        </DialogTitle>
        {project?.href?.includes("github.com") && project.href && (
          <DialogDescription>
            <a
              href={project.href}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.github_link}
              title={t("projects.visit-github-repo", {
                title: project?.title ?? "",
              })}
              aria-label={t("projects.visit-github-repo", {
                title: project?.title ?? "",
              })}
            >
              {project.href}
            </a>
          </DialogDescription>
        )}
      </DialogHeader>
      <DialogBody>
        <ScrollArea maxHeight="60vh" className={styles.scroll_area}>
          <MarkdownViewer>{markdown}</MarkdownViewer>
        </ScrollArea>
      </DialogBody>
      <DialogFooter>
        <Button
          variant="secondary"
          title={t("projects.close")}
          aria-label={t("projects.close")}
          onClick={handleClose}
        >
          {t("projects.close")}
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default memo(ProjectDetailDialog);
