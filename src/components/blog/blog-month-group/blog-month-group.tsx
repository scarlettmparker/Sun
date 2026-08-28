import { useTranslation } from "react-i18next";
import { Card, CardBody, CardHeader, CardTitle } from "@sun/components";
import type { BlogPost } from "~/generated/graphql";
import BlogPostRow from "~/components/blog/blog-post-row";
import styles from "./blog-month-group.module.css";

type BlogMonthGroupProps = {
  /**
   * Month label e.g. August 2025.
   */
  monthYear: string;
  /**
   * Posts for this month.
   */
  posts: BlogPost[];
};

/**
 * Renders a month group as a single card.
 */
const BlogMonthGroup = (props: BlogMonthGroupProps) => {
  const { monthYear, posts } = props;
  const { t } = useTranslation("blog");

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t(monthYear)}</CardTitle>
      </CardHeader>
      <CardBody>
        <div className={styles.list_body}>
          {posts.map((post) => (
            <BlogPostRow key={post.id} post={post} />
          ))}
        </div>
      </CardBody>
    </Card>
  );
};

export default BlogMonthGroup;
