import { MarkdownViewer, Card, CardBody } from "@sun/components";
import { LocateBlogPostQuery } from "~/generated/graphql";
import { usePageData } from "@sun/ssr/react";
import { useParams } from "react-router-dom";
import styles from "./blog-post.module.css";

const BlogPostPage = () => {
  const { id } = useParams<{ id: string }>();
  const { data } = usePageData<
    LocateBlogPostQuery["blogQueries"]["locateBlogPost"]
  >("blogPost", "blog/:id", { id });

  if (!data?.content) {
    return null;
  }

  return (
    <div className={styles.blog_post}>
      <Card>
        <CardBody>
          <MarkdownViewer className={styles.blog_body}>
            {data.content}
          </MarkdownViewer>
        </CardBody>
      </Card>
    </div>
  );
};

export default BlogPostPage;
