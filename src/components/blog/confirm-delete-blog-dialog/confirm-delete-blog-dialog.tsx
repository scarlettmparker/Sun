import { TFunction } from "i18next";
import {
  Button,
  Dialog,
  DialogBody,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@sun/components";
import type { BlogPost } from "~/generated/graphql";
import { deleteBlogPost } from "~/server/actions/blog-post";

type ConfirmDeleteBlogDialogProps = {
  /**
   * Whether the dialog is open.
   */
  open: boolean;
  /**
   * Called to close the dialog without confirming.
   */
  onClose: () => void;
  /**
   * The post to delete (null when closed).
   */
  post: BlogPost | null;
  /**
   * i18n translation function for the blog namespace.
   */
  t: TFunction<"blog">;
};

/**
 * Confirmation dialog for permanently deleting a blog post and its children.
 */
const ConfirmDeleteBlogDialog = (props: ConfirmDeleteBlogDialogProps) => {
  const { open, onClose, post, t } = props;

  if (!post) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={(o: boolean) => !o && onClose()}>
      <DialogHeader>
        <DialogTitle>{t("detail.delete-title")}</DialogTitle>
      </DialogHeader>
      <DialogBody>
        <p>{t("detail.delete-body", { title: post.title })}</p>
      </DialogBody>
      <DialogFooter>
        <Button type="button" variant="secondary" onClick={onClose}>
          {t("cancel")}
        </Button>
        <Button
          type="submit"
          variant="destructive"
          onClick={() => {
            onClose();
            deleteBlogPost(post.id);
          }}
        >
          {t("detail.delete-submit")}
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default ConfirmDeleteBlogDialog;
