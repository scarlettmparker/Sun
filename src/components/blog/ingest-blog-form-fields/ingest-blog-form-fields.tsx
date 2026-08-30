import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Form, FormField, FormItem, FormLabel, Input, Select, SelectOption } from "@sun/components";
import type { BlogPostTypesQuery } from "~/generated/graphql";

type IngestBlogFormFieldsProps = {
  /**
   * Selected source kind.
   */
  sourceKind: "WIKIPEDIA" | "WIKTIONARY";
  /**
   * Callback for source kind change.
   */
  onSourceKindChange: (value: "WIKIPEDIA" | "WIKTIONARY") => void;
  /**
   * Submit handler.
   */
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  /**
   * Default type name to pre-select.
   */
  defaultTypeName?: string | null;
};

/**
 * Renders ingest form fields.
 */
const IngestBlogFormFields = (props: IngestBlogFormFieldsProps) => {
  const { sourceKind, onSourceKindChange, onSubmit, defaultTypeName } = props;
  const { t } = useTranslation("blog");
  const { data: types } = usePageData<NonNullable<BlogPostTypesQuery["blogQueries"]["blogPostTypes"]>>(
    "types",
    "home",
    {},
  );
  const typeOptions = types ?? [];

  return (
    <Form id="ingest-blog-form" onSubmit={onSubmit}>
      <FormField name="title">
        <FormLabel>{t("ingest.title-label")}</FormLabel>
        <FormItem>
          <Input type="text" placeholder={t("ingest.title-placeholder")} required />
        </FormItem>
      </FormField>
      <FormField name="typeName">
        <FormLabel>{t("ingest.type-label")}</FormLabel>
        <FormItem>
          <Select defaultValue={defaultTypeName ?? typeOptions[0]?.name ?? "KNOWLEDGE"}>
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
              onSourceKindChange(event.target.value as "WIKIPEDIA" | "WIKTIONARY")
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
          <Input type="text" placeholder={t("ingest.source-id-placeholder")} required />
        </FormItem>
      </FormField>
    </Form>
  );
};

export default IngestBlogFormFields;
