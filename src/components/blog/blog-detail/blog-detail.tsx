import { useParams } from "react-router-dom";
import { usePageData } from "@sun/ssr/react";
import { Card, CardBody, CardHeader, CardTitle, MarkdownViewer } from "@sun/components";
import type { LocateBlogPostQuery } from "~/generated/graphql";
import BlogBreadcrumb from "~/components/blog/blog-breadcrumb";
import KnowledgeGraph from "~/components/knowledge/knowledge-graph";
import styles from "./blog-detail.module.css";

type BlogDetailProps = Record<string, never>;

/**
 * Renders a single blog post with breadcrumb.
 */
const BlogDetail = (props: BlogDetailProps) => {
  const {} = props;
  const { id } = useParams<{ id: string }>();
  const { data } = usePageData<LocateBlogPostQuery["blogQueries"]["locateBlogPost"]>(
    "blogPost",
    "blog/:id",
    { id },
  );

  if (!data?.content) {
    return null;
  }

  return (
    <div className={styles.detail_wrapper}>
      <BlogBreadcrumb post={data} />
      <Card>
        <CardHeader>
          <CardTitle>{data.title}</CardTitle>
        </CardHeader>
        <CardBody>
          <MarkdownViewer className={styles.blog_body}>{data.content}</MarkdownViewer>
        </CardBody>
      </Card>
      <KnowledgeGraph post={data} />
    </div>
  );
};

export default BlogDetail;
