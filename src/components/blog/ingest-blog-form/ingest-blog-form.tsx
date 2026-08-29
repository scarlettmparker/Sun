import { Suspense, useState, useTransition } from "react";
import { useTranslation } from "react-i18next";
import { Button, Card, CardBody, CardHeader, CardTitle } from "@sun/components";
import { ingestBlogFromSource } from "~/server/actions/blog-post";
import IngestBlogFormFields from "~/components/blog/ingest-blog-form-fields";
import IngestBlogFormFieldsSkeleton from "./skeletons/ingest-blog-form-fields-skeleton";
import styles from "./ingest-blog-form.module.css";

/**
 * Form to ingest a blog from wikipedia or wiktionary.
 */
const IngestBlogForm = () => {
  const { t } = useTranslation("blog");
  const [isPending, startTransition] = useTransition();
  const [sourceKind, setSourceKind] = useState<"WIKIPEDIA" | "WIKTIONARY">("WIKIPEDIA");

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
      });
    });
  };

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>{t("ingest.title")}</CardTitle>
        </CardHeader>
        <CardBody>
          <Suspense fallback={<IngestBlogFormFieldsSkeleton />}>
            <IngestBlogFormFields
              sourceKind={sourceKind}
              onSourceKindChange={setSourceKind}
              onSubmit={handleSubmit}
            />
          </Suspense>
        </CardBody>
      </Card>
      <div className={styles.form_actions}>
        <Button
          type="submit"
          form="ingest-blog-form"
          title={isPending ? t("ingest.submitting-label") : t("ingest.submit-label")}
          aria-label={isPending ? t("ingest.submitting-label") : t("ingest.submit-label")}
          disabled={isPending}
        >
          {isPending ? t("ingest.submitting-label") : t("ingest.submit-label")}
        </Button>
      </div>
    </>
  );
};

export default IngestBlogForm;
