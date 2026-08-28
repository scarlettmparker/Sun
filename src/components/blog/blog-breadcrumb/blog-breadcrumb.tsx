import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Breadcrumb, BreadcrumbProvider, useBreadcrumbContext } from "@sun/components";

type BlogBreadcrumbProps = {
  /**
   * Type name for back link.
   */
  typeName: string | null;
};

type InnerProps = {
  /**
   * Type name for back link.
   */
  typeName: string | null;
};

/**
 * Inner breadcrumb that syncs crumbs to context.
 */
const BlogBreadcrumbInner = (props: InnerProps) => {
  const { typeName } = props;
  const { t } = useTranslation("blog");
  const { setBreadcrumbs, setCurrent } = useBreadcrumbContext();
  const href = typeName ? `/blog?type=${encodeURIComponent(typeName)}` : "/blog";

  useEffect(() => {
    const typeLabel = typeName ? t(`types.${typeName}`, { defaultValue: typeName }) : null;
    const crumbs = [{ label: t("breadcrumb.blog"), href: "/blog" }];
    if (typeLabel && typeName) {
      crumbs.push({ label: typeLabel, href });
    }
    setBreadcrumbs(crumbs);
    setCurrent(href);
  }, [typeName, t, setBreadcrumbs, setCurrent, href]);

  return <Breadcrumb separator="/" />;
};

/**
 * Breadcrumb for blog post detail.
 */
const BlogBreadcrumb = (props: BlogBreadcrumbProps) => {
  const { typeName } = props;

  return (
    <BreadcrumbProvider>
      <BlogBreadcrumbInner typeName={typeName} />
    </BreadcrumbProvider>
  );
};

export default BlogBreadcrumb;
