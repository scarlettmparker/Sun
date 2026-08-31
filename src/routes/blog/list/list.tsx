import { Suspense, useState } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "@sun/components";
import { ArrowDownTrayIcon, PlusIcon } from "@heroicons/react/24/outline";
import BlogList from "~/components/blog/blog-list";
import { BlogListSkeleton } from "~/components/blog/skeletons";
import IngestBlogDialog from "~/components/blog/ingest-blog-dialog";
import styles from "./list.module.css";

/**
 * Blog list outlet page.
 */
const BlogListPage = () => {
  const { t } = useTranslation("blog");
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const selectedType = searchParams.get("type");

  const createHref =
    selectedType == null
      ? `/blog/create?from=${encodeURIComponent(location.pathname + location.search)}`
      : `/blog/create?from=${encodeURIComponent(location.pathname + location.search)}&type=${encodeURIComponent(selectedType)}`;
  const [ingestOpen, setIngestOpen] = useState(false);

  return (
    <div className={styles.list_page}>
      <Suspense fallback={<BlogListSkeleton />}>
        <BlogList selectedType={selectedType} />
      </Suspense>
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
          title={t("form.create.title")}
          aria-label={t("form.create.title")}
          onClick={() => navigate(createHref)}
        >
          <PlusIcon className={styles.action_icon} width={16} height={16} />
          <span>{t("form.create.label")}</span>
        </Button>
      </div>
      <IngestBlogDialog
        open={ingestOpen}
        onOpenChange={setIngestOpen}
        parentId={null}
        parentTypeName={selectedType}
      />
    </div>
  );
};

export default BlogListPage;
