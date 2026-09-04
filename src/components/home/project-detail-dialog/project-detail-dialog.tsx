import { useTranslation } from "react-i18next";
import {
  Button,
  Dialog,
  DialogBody,
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
  const markdown = detailKey ? t(detailKey) : "";

  return (
    <Dialog open={open} onOpenChange={onOpenChange} className={styles.dialog}>
      <DialogHeader>
        <DialogTitle>{project?.title ?? ""}</DialogTitle>
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
          onClick={() => onOpenChange(false)}
        >
          {t("projects.close")}
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default ProjectDetailDialog;
