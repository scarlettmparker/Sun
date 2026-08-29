import { useTranslation } from "react-i18next";
import {
  FormField,
  FormItem,
  FormLabel,
  Input,
  MarkdownEditor,
  Select,
  SelectOption,
} from "@sun/components";
import { usePageData } from "@sun/ssr/react";
import { useSearchParams } from "react-router-dom";
import type { BlogPostTypesQuery } from "~/generated/graphql";

/**
 * Fields for creating a new blog post.
 */
const CreateBlogForm = () => {
  const { t } = useTranslation("blog");
  const [searchParams] = useSearchParams();
  const queryType = searchParams.get("type");

  const { data: types } = usePageData<BlogPostTypesQuery["blogQueries"]["blogPostTypes"]>(
    "types",
    "home",
    {},
  );

  const defaultTypeId =
    queryType == null
      ? ""
      : (types ?? []).find((postType) => postType.name === queryType)?.id ?? "";

  const DEFAULT_ROWS = 10;

  return (
    <>
      <FormField name="title">
        <FormLabel>{t("form.title.label")}</FormLabel>
        <FormItem>
          <Input type="text" placeholder={t("form.title.placeholder")} required />
        </FormItem>
      </FormField>
      <FormField name="typeId">
        <FormLabel>{t("form.type.label")}</FormLabel>
        <FormItem>
          <Select key={defaultTypeId} name="typeId" defaultValue={defaultTypeId}>
            <SelectOption value="">{t("form.type.placeholder")}</SelectOption>
            {(types ?? [])
              .filter((postType) => postType.name !== "BOT_FAQ" && postType.name !== "BOT_HELP")
              .map((postType) => (
                <SelectOption key={postType.id} value={postType.id}>
                  {t(`types.${postType.name}`, { defaultValue: postType.name })}
                </SelectOption>
              ))}
          </Select>
        </FormItem>
      </FormField>
      <FormField name="content">
        <FormLabel>{t("form.content.label")}</FormLabel>
        <FormItem>
          <MarkdownEditor
            placeholder={t("form.content.placeholder")}
            rows={DEFAULT_ROWS}
            data-testid="create-blog-content-editor"
            aria-label={t("form.content.label")}
          />
        </FormItem>
      </FormField>
    </>
  );
};

export default CreateBlogForm;
