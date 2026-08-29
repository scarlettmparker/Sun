import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  Form,
  FormField,
  FormFooter,
  FormItem,
  FormLabel,
  Input,
  MarkdownEditor,
  Select,
  SelectOption,
} from "@sun/components";
import { usePageData } from "@sun/ssr/react";
import { Link, useSearchParams } from "react-router-dom";
import type { BlogPostTypesQuery } from "~/generated/graphql";
import { createBlogPost } from "~/server/actions/blog-post";

/**
 * Form for creating a new blog post.
 */
const CreateBlogForm = () => {
  const { t } = useTranslation("blog");
  const [searchParams] = useSearchParams();
  const cancelTo = searchParams.get("from") || "/blog";
  const parentId = searchParams.get("parentId") || undefined;

  const { data: types } = usePageData<BlogPostTypesQuery["blogQueries"]["blogPostTypes"]>(
    "types",
    "home",
    {},
  );

  const DEFAULT_ROWS = 10;

  const [loading, setLoading] = useState(false);
  const [_error, setError] = useState<string | null>(null);
  const [_success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (loading) return;
    setLoading(true);
    setError(null);
    setSuccess(false);

    const formData = new FormData(e.currentTarget);
    const title = formData.get("title") as string;
    const content = formData.get("content") as string;
    const typeId = formData.get("typeId") as string;

    const result = await createBlogPost(
      title,
      content,
      typeId || undefined,
      parentId,
    );

    if (result.__typename === "QuerySuccess") {
      setSuccess(true);
    } else if (result.__typename === "StandardError") {
      setError(result.message);
    }

    setLoading(false);
  };

  return (
    <Form onSubmit={handleSubmit} data-testid="create-blog-form">
      <FormField name="title">
        <FormLabel>{t("form.title.label")}</FormLabel>
        <FormItem>
          <Input type="text" placeholder={t("form.title.placeholder")} required />
        </FormItem>
      </FormField>
      <FormField name="typeId">
        <FormLabel>{t("form.type.label")}</FormLabel>
        <FormItem>
          <Select name="typeId" defaultValue="">
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
      <FormFooter>
        <Link to={cancelTo}>
          <Button type="button" variant="secondary" title={t("form.cancel.title")}>
            {t("form.cancel.label")}
          </Button>
        </Link>
        <Button
          type="submit"
          title={loading ? t("form.creating.title") : t("form.create.title")}
          disabled={loading}
          data-testid="create-blog-submit-button"
        >
          {loading ? t("form.creating.label") : t("form.create.label")}
        </Button>
      </FormFooter>
    </Form>
  );
};

export default CreateBlogForm;
