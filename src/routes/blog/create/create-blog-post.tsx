import { Suspense, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { BreadcrumbItem, Button, Card, CardBody, Form } from "@sun/components";
import CreateBlogForm from "~/_components/blog/create/";
import { CreateBlogSkeleton } from "~/_components/blog/create/skeletons";
import ParentBreadcrumb from "~/components/blog/parent-breadcrumb";
import breadcrumbStyles from "~/components/blog/parent-breadcrumb/parent-breadcrumb.module.css";
import { createBlogPost } from "~/server/actions/blog-post";
import styles from "./create-blog-post.module.css";

/**
 * Page for creating a new blog post.
 */
const CreateBlogPostPage = () => {
  const { t } = useTranslation("blog");
  const [searchParams] = useSearchParams();
  const parentId = searchParams.get("parentId");
  const cancelTo = searchParams.get("from") || "/blog";
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (loading) return;
    setLoading(true);

    const formData = new FormData(e.currentTarget);
    const title = formData.get("title") as string;
    const content = formData.get("content") as string;
    const typeId = formData.get("typeId") as string;

    await createBlogPost(
      title,
      content,
      typeId || undefined,
      parentId || undefined,
    );

    setLoading(false);
  };

  return (
    <div className={styles.create_blog_page}>
      {parentId == null ? (
        <nav aria-label="breadcrumb">
          <ol className={breadcrumbStyles.breadcrumb}>
            <BreadcrumbItem>
              <Link to="/blog">{t("breadcrumb.blog")}</Link>
            </BreadcrumbItem>
            <BreadcrumbItem active>{t("breadcrumb.new")}</BreadcrumbItem>
          </ol>
        </nav>
      ) : (
        <Suspense
          fallback={
            <nav aria-label="breadcrumb">
              <ol className={breadcrumbStyles.breadcrumb}>
                <BreadcrumbItem>
                  <Link to="/blog">{t("breadcrumb.blog")}</Link>
                </BreadcrumbItem>
                <BreadcrumbItem active>{t("breadcrumb.new")}</BreadcrumbItem>
              </ol>
            </nav>
          }
        >
          <ParentBreadcrumb parentId={parentId} />
        </Suspense>
      )}
      <Card>
        <CardBody>
          <Suspense fallback={<CreateBlogSkeleton />}>
            <Form
              id="create-blog-form"
              onSubmit={handleSubmit}
              data-testid="create-blog-form"
            >
              <CreateBlogForm />
            </Form>
          </Suspense>
        </CardBody>
      </Card>
      <div className={styles.form_actions}>
        <Link to={cancelTo}>
          <Button
            type="button"
            variant="secondary"
            title={t("form.cancel.title")}
          >
            {t("form.cancel.label")}
          </Button>
        </Link>
        <Button
          type="submit"
          title={loading ? t("form.creating.title") : t("form.create.title")}
          disabled={loading}
          data-testid="create-blog-submit-button"
          form="create-blog-form"
        >
          {loading ? t("form.creating.label") : t("form.create.label")}
        </Button>
      </div>
    </div>
  );
};

export default CreateBlogPostPage;
