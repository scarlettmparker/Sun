import { Suspense } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
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
  const selectedType = searchParams.get("type");

  return (
    <div className={styles.list_page}>
      <div className={styles.toolbar}>
        <Link
          to={`/blog/create?from=${encodeURIComponent(location.pathname + location.search)}`}
          className={styles.create_button}
        >
          <Button title={t("form.create.title")} aria-label={t("form.create.title")}>
            <PlusIcon width={16} height={16} />
            {t("form.create.label")}
          </Button>
        </Link>
      </div>
      <Suspense fallback={<BlogListSkeleton />}>
        <BlogList selectedType={selectedType} />
      </Suspense>
    </div>
  );
};

export default BlogListPage;
