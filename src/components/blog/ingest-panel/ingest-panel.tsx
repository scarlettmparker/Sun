import { useState, useTransition } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Form,
  FormField,
  FormItem,
  FormLabel,
  Input,
  Select,
  SelectOption,
} from "@sun/components";
import { ingestBlogFromSource } from "~/server/actions/blog-post";
import { usePageData } from "@sun/ssr/react";
import type { BlogPostTypesQuery } from "~/generated/graphql";
import styles from "./ingest-panel.module.css";

/**
 * Form to ingest a blog from wikipedia or wiktionary.
 */
const IngestPanel = () => {
  const { t } = useTranslation("blog");
  const [isPending, startTransition] = useTransition();
  const [sourceKind, setSourceKind] = useState<"WIKIPEDIA" | "WIKTIONARY">(
    "WIKIPEDIA",
  );
  const { data: types } = usePageData<
    NonNullable<BlogPostTypesQuery["blogQueries"]["blogPostTypes"]>
  >("types", "home", {});
  const typeOptions = types ?? [];

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
          <Form id="ingest-panel-form" onSubmit={handleSubmit}>
            <FormField name="title">
              <FormLabel>{t("ingest.title-label")}</FormLabel>
              <FormItem>
                <Input
                  type="text"
                  placeholder={t("ingest.title-placeholder")}
                  required
                />
              </FormItem>
            </FormField>
            <FormField name="typeName">
              <FormLabel>{t("ingest.type-label")}</FormLabel>
              <FormItem>
                <Select defaultValue={typeOptions[0]?.name ?? "KNOWLEDGE"}>
                  {typeOptions.map((option) => (
                    <SelectOption key={option.id} value={option.name}>
                      {option.name}
                    </SelectOption>
                  ))}
                </Select>
              </FormItem>
            </FormField>
            <FormField name="sourceKind">
              <FormLabel>{t("ingest.source-kind-label")}</FormLabel>
              <FormItem>
                <Select
                  value={sourceKind}
                  onChange={(event: React.ChangeEvent<HTMLSelectElement>) =>
                    setSourceKind(
                      event.target.value as "WIKIPEDIA" | "WIKTIONARY",
                    )
                  }
                >
                  <SelectOption value="WIKIPEDIA">Wikipedia</SelectOption>
                  <SelectOption value="WIKTIONARY">Wiktionary</SelectOption>
                </Select>
              </FormItem>
            </FormField>
            <FormField name="sourceId">
              <FormLabel>{t("ingest.source-id-label")}</FormLabel>
              <FormItem>
                <Input
                  type="text"
                  placeholder={t("ingest.source-id-placeholder")}
                  required
                />
              </FormItem>
            </FormField>
          </Form>
        </CardBody>
      </Card>
      <div className={styles.form_actions}>
        <Button
          type="submit"
          form="ingest-panel-form"
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
      </div>
    </>
  );
};

export default IngestPanel;
