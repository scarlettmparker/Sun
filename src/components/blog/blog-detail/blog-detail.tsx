import { useState } from "react";
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
  ScrollArea,
} from "@sun/components";
import { ArrowDownTrayIcon, PlusIcon, TrashIcon } from "@heroicons/react/24/outline";
import type { BlogPost, LocateBlogPostQuery } from "~/generated/graphql";
import BlogBreadcrumb from "~/components/blog/blog-breadcrumb";
import KnowledgeGraph from "~/components/knowledge/knowledge-graph";
import AttachTextPicker from "~/components/blog/attach-text-picker";
import IngestBlogDialog from "~/components/blog/ingest-blog-dialog";
import ConfirmDeleteBlogDialog from "~/components/blog/confirm-delete-blog-dialog";
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
  const [ingestOpen, setIngestOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  if (data?.content) {
    // fall through
  } else {
    return null;
  }

  const handleCreateChild = () => {
    const typeParam =
      data.type?.name == null
        ? ""
        : `&type=${encodeURIComponent(data.type.name)}`;
    navigate(
      `/blog/create?parentId=${id}&from=${encodeURIComponent(location.pathname)}${typeParam}`,
    );
  };

  return (
    <div className={styles.detail_wrapper}>
      <BlogBreadcrumb post={data as BlogPost} />
      <div className={styles.columns}>
        <div className={styles.left_column}>
          <Card>
            <CardHeader>
              <CardTitle>{data.title}</CardTitle>
            </CardHeader>
            <CardBody>
              <ScrollArea maxHeight="36rem">
                <MarkdownViewer className={styles.blog_body}>
                  {data.content}
                </MarkdownViewer>
              </ScrollArea>
            </CardBody>
          </Card>
          <KnowledgeGraph post={data as BlogPost} />
        </div>
        <div className={styles.right_column}>
          <AttachTextPicker postId={data.id} />
        </div>
      </div>
      <div className={styles.actions}>
        <Button
          variant="secondary"
          title={t("ingest.open-label")}
          aria-label={t("ingest.open-label")}
          onClick={() => setIngestOpen(true)}
        >
          <ArrowDownTrayIcon
            className={styles.action_icon}
            width={16}
            height={16}
          />
          <span>{t("ingest.open-label")}</span>
        </Button>
        <Button
          variant="secondary"
          title={t("detail.delete-title")}
          aria-label={t("detail.delete-title")}
          onClick={() => setDeleteOpen(true)}
        >
          <TrashIcon className={styles.action_icon} width={16} height={16} />
          <span>{t("detail.delete-submit")}</span>
        </Button>
        <Button
          title={t("detail.create-child.title")}
          aria-label={t("detail.create-child.title")}
          onClick={handleCreateChild}
        >
          <PlusIcon className={styles.action_icon} width={16} height={16} />
          <span>{t("form.create.label")}</span>
        </Button>
      </div>
      <IngestBlogDialog
        open={ingestOpen}
        onOpenChange={setIngestOpen}
        parentId={id}
        parentTypeName={data.type?.name ?? null}
      />
      <ConfirmDeleteBlogDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        post={data as BlogPost}
        t={t}
      />
    </div>
  );
};

export default BlogDetail;
