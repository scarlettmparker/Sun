import { Suspense } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "@sun/components";
import { PlusIcon } from "@heroicons/react/24/outline";
import BlogList from "~/components/blog/blog-list";
import { BlogListSkeleton } from "~/components/blog/skeletons";
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

  return (
    <div className={styles.list_page}>
      <Suspense fallback={<BlogListSkeleton />}>
        <BlogList selectedType={selectedType} />
      </Suspense>
      <Button
        className={styles.create_button}
        title={t("form.create.title")}
        aria-label={t("form.create.title")}
        onClick={() => navigate(createHref)}
      >
        <PlusIcon className={styles.create_icon} width={16} height={16} />
        <span>{t("form.create.label")}</span>
      </Button>
    </div>
  );
};

export default BlogListPage;
