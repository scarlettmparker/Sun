import { useTranslation } from "react-i18next";
import {
  Button,
  Dialog,
  DialogBody,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@sun/components";
import type { Project } from "../projects/projects-config";

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
 * Placeholder dialog for project details.
 */
const ProjectDetailDialog = (props: ProjectDetailDialogProps) => {
  const { project, onOpenChange } = props;
  const { t } = useTranslation("home");

  const open = project != null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogHeader>
        <DialogTitle>{project?.title ?? ""}</DialogTitle>
      </DialogHeader>
      <DialogBody>
        <p>{t("projects.detail-placeholder")}</p>
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
