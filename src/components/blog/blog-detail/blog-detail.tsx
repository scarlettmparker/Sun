import { Suspense } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  MarkdownViewer,
} from "@sun/components";
import { PlusIcon } from "@heroicons/react/24/outline";
import type { LocateBlogPostQuery } from "~/generated/graphql";
import BlogBreadcrumb from "~/components/blog/blog-breadcrumb";
import KnowledgeGraph from "~/components/knowledge/knowledge-graph";
import AttachTextDialog from "~/components/blog/attach-text-dialog";
import IngestPanel from "~/components/blog/ingest-panel";
import styles from "./blog-detail.module.css";

/**
 * Renders a single blog post with breadcrumb.
 */
const BlogDetail = () => {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const { t } = useTranslation("blog");
  const { data } = usePageData<
    LocateBlogPostQuery["blogQueries"]["locateBlogPost"]
  >("blogPost", "blog/:id", { id });

  if (data?.content) {
    // fall through
  } else {
    return null;
  }

  return (
    <div className={styles.detail_wrapper}>
      <BlogBreadcrumb post={data} />
      <div className={styles.columns}>
        <div className={styles.left_column}>
          <Card>
            <CardHeader>
              <CardTitle>{data.title}</CardTitle>
            </CardHeader>
            <CardBody>
              <MarkdownViewer className={styles.blog_body}>
                {data.content}
              </MarkdownViewer>
            </CardBody>
          </Card>
          <KnowledgeGraph post={data} />
        </div>
        <div className={styles.right_column}>
          <Suspense fallback={null}>
            <AttachTextDialog postId={data.id} />
          </Suspense>
          <Suspense fallback={null}>
            <IngestPanel />
          </Suspense>
        </div>
      </div>
      <Button
        className={styles.create_button}
        title={t("detail.create-child.title")}
        aria-label={t("detail.create-child.title")}
        onClick={() => {
          const typeParam =
            data.type?.name == null
              ? ""
              : `&type=${encodeURIComponent(data.type.name)}`;
          navigate(
            `/blog/create?parentId=${id}&from=${encodeURIComponent(location.pathname)}${typeParam}`,
          );
        }}
      >
        <PlusIcon className={styles.create_icon} width={16} height={16} />
        <span>{t("form.create.label")}</span>
      </Button>
    </div>
  );
};

export default BlogDetail;
