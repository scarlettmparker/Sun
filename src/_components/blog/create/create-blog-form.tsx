import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Form,
  FormField,
  FormLabel,
  FormItem,
  FormFooter,
} from "~/components/form";
import Input from "~/components/input";
import { createBlogPost } from "~/server/actions/blog-post";
import Button from "~/components/button";
import styles from "./create-blog-form.module.css";
import MarkdownEditor from "~/components/markdown-editor";

/**
 * Form for creating a new blog post.
 */
const CreateBlogForm = () => {
  const { t } = useTranslation("blog");

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

    const result = await createBlogPost(title, content);

    if (result.__typename === "QuerySuccess") {
      setSuccess(true);
    } else if (result.__typename === "StandardError") {
      setError(result.message);
    }

    setLoading(false);
  };

  return (
    <Form
      onSubmit={handleSubmit}
      className={styles.create_blog_form}
      data-testid="create-blog-form"
    >
      <FormField name="title">
        <FormLabel>{t("form.title.label")}</FormLabel>
        <FormItem>
          <Input type="text" placeholder={t("form.title.placeholder")} />
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
        <a href="/blog">
          <Button
            type="button"
            variant="secondary"
            title={t("form.cancel.title")}
          >
            {t("form.cancel.label")}
          </Button>
        </a>
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
