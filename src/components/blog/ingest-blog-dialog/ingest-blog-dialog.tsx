import { Suspense, useState, useTransition } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  Dialog,
  DialogBody,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@sun/components";
import { ingestBlogFromSource } from "~/server/actions/blog-post";
import IngestBlogFormFields from "~/components/blog/ingest-blog-form-fields";
import IngestBlogFormFieldsSkeleton from "~/components/blog/ingest-blog-form/skeletons/ingest-blog-form-fields-skeleton";

type IngestBlogDialogProps = {
  /**
   * Whether the dialog is open.
   */
  open: boolean;
  /**
   * Callback for open changes.
   */
  onOpenChange: (open: boolean) => void;
  /**
   * Optional parent id for child creation.
   */
  parentId?: string | null;
  /**
   * Optional parent type name for default.
   */
  parentTypeName?: string | null;
};

/**
 * Dialog to ingest a blog from Wikipedia or Wiktionary, optionally as a child.
 */
const IngestBlogDialog = (props: IngestBlogDialogProps) => {
  const { open, onOpenChange, parentId, parentTypeName } = props;
  const { t } = useTranslation("blog");
  const [isPending, startTransition] = useTransition();
  const [sourceKind, setSourceKind] = useState<"WIKIPEDIA" | "WIKTIONARY">(
    "WIKIPEDIA",
  );

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const title = (formData.get("title") as string) ?? "";
    const typeName = (formData.get("typeName") as string) ?? "";
    const sourceId = (formData.get("sourceId") as string) ?? "";
    if (!title.trim() || !typeName || !sourceId.trim()) {
      return;
    }
    startTransition(async () => {
      await ingestBlogFromSource({
        title: title.trim(),
        typeName,
        sourceKind,
        sourceId: sourceId.trim(),
        parentId: parentId ?? null,
      });
      onOpenChange(false);
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogHeader>
        <DialogTitle>{t("ingest.title")}</DialogTitle>
      </DialogHeader>
      <DialogBody>
        <Suspense fallback={<IngestBlogFormFieldsSkeleton />}>
          <IngestBlogFormFields
            sourceKind={sourceKind}
            onSourceKindChange={setSourceKind}
            onSubmit={handleSubmit}
            defaultTypeName={parentTypeName}
          />
        </Suspense>
      </DialogBody>
      <DialogFooter>
        <Button variant="secondary" onClick={() => onOpenChange(false)}>
          {t("cancel")}
        </Button>
        <Button
          type="submit"
          form="ingest-blog-form"
          title={
            isPending ? t("ingest.submitting-label") : t("ingest.submit-label")
          }
          aria-label={
            isPending ? t("ingest.submitting-label") : t("ingest.submit-label")
          }
          disabled={isPending}
        >
          {isPending ? t("ingest.submitting-label") : t("ingest.submit-label")}
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default IngestBlogDialog;
