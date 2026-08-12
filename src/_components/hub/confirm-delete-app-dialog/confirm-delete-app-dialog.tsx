import { useTranslation } from "react-i18next";
import {
  Button,
  Dialog,
  DialogBody,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@sun/components";
import { useHub } from "~/routes/hub/hub-context";

/**
 * Confirmation dialog for deleting a hub app.
 */
const ConfirmDeleteAppDialog = () => {
  const { t } = useTranslation("hub");
  const { deleting, confirmDelete, closeDelete } = useHub();

  return (
    <Dialog open={Boolean(deleting)} onOpenChange={(o: boolean) => !o && closeDelete()}>
      <DialogHeader>
        <DialogTitle>
          {t("delete-title", { app: deleting?.name ?? "" })}
        </DialogTitle>
      </DialogHeader>
      <DialogBody>
        <p>{t("delete-body", { app: deleting?.name ?? "" })}</p>
      </DialogBody>
      <DialogFooter>
        <Button type="button" variant="secondary" onClick={closeDelete}>
          {t("cancel")}
        </Button>
        <Button type="button" variant="destructive" onClick={confirmDelete}>
          {t("delete")}
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default ConfirmDeleteAppDialog;
