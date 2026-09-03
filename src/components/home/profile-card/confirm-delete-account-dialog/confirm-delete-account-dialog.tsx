import { useTranslation } from "react-i18next";
import {
  Button,
  Dialog,
  DialogBody,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@sun/components";

type ConfirmDeleteAccountDialogProps = {
  /**
   * Whether dialog is open.
   */
  open: boolean;
  /**
   * Called when open state changes.
   */
  onOpenChange: (open: boolean) => void;
  /**
   * Whether deletion is pending.
   */
  isPending: boolean;
  /**
   * Called when deletion is confirmed.
   */
  onConfirm: () => void;
};

/**
 * Confirmation dialog for deleting the current account.
 */
const ConfirmDeleteAccountDialog = (
  props: ConfirmDeleteAccountDialogProps,
) => {
  const { open, onOpenChange, isPending, onConfirm } = props;
  const { t } = useTranslation("home");

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogHeader>
        <DialogTitle>{t("profile.delete-title")}</DialogTitle>
      </DialogHeader>
      <DialogBody>
        <p>{t("profile.delete-body")}</p>
      </DialogBody>
      <DialogFooter>
        <Button
          variant="secondary"
          title={t("profile.cancel")}
          aria-label={t("profile.cancel")}
          onClick={() => onOpenChange(false)}
        >
          {t("profile.cancel")}
        </Button>
        <Button
          variant="destructive"
          title={t("profile.delete")}
          aria-label={t("profile.delete")}
          disabled={isPending}
          onClick={onConfirm}
        >
          {isPending ? t("profile.deleting") : t("profile.delete")}
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default ConfirmDeleteAccountDialog;
