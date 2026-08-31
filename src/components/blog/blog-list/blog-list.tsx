import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Card, CardBody } from "@sun/components";
import type { ListBlogPostsQuery } from "~/generated/graphql";
import { groupPostsByMonthYear } from "~/routes/blog/group-posts-by-month-year";
import BlogMonthGroup from "~/components/blog/blog-month-group";
import styles from "./blog-list.module.css";

type BlogListProps = {
  /**
   * Selected type name to filter by.
   */
  selectedType: string | null;
};

/**
 * Renders blog posts grouped by month as separate cards.
 */
const BlogList = (props: BlogListProps) => {
  const { selectedType } = props;
  const { t } = useTranslation("blog");
  const filters = [];
  if (selectedType) {
    filters.push({
      field: "type.name",
      operator: "EQUALS" as const,
      value: selectedType,
    });
  }
  const pagination = {
    page: 0,
    size: 50,
    filters,
    sorts: [
      { field: "lastUpdatedAt", dir: "DESC" as const },
      { field: "title", dir: "ASC" as const },
    ],
  };
  const { data: initialData } = usePageData<
    ListBlogPostsQuery["blogQueries"]["listBlogPosts"]["items"]
  >("blogPosts", "blog", { pagination });

  const groupedPosts = groupPostsByMonthYear(initialData ?? []);

  if (!groupedPosts.length) {
    return (
      <Card>
        <CardBody>
          <p className={styles.no_items}>{t("filter.empty")}</p>
        </CardBody>
      </Card>
    );
  }

  return (
    <div className={styles.groups}>
      {groupedPosts.map((group) => (
        <BlogMonthGroup
          key={group.monthYear}
          monthYear={group.monthYear}
          posts={group.posts}
        />
      ))}
    </div>
  );
};

export default BlogList;
