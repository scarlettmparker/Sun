import MarkdownViewer from "~/components/markdown-viewer";
import { LocateBlogPostQuery } from "~/generated/graphql";
import { fetchLocateBlogPost } from "~/utils/api";
import { useParams } from "react-router-dom";

import { pageDataRegistry, getPageData } from "~/utils/page-data";
import styles from "./blog-post.module.css";
import Card, { CardBody } from "~/components/card";

const BlogPostPage = () => {
  const { id } = useParams<{ id: string }>();
  const { data } = getPageData<
    LocateBlogPostQuery["blogQueries"]["locateBlogPost"]
  >("blogPost", "blog/:id", { id });

  if (!data) {
    // TODO: suspense properly
    return <div>Loading...</div>;
  }

  return (
    <div className={styles.blog_post}>
      <Card>
        <CardBody>
          <MarkdownViewer>{data.content}</MarkdownViewer>
        </CardBody>
      </Card>
    </div>
  );
};

/**
 * Data fetching function for BlogPostPage.
 * @param id The blog post ID from the route.
 * @returns Promise resolving to page data or null if no data.
 */
export async function getBlogPostDetails(
  id: string
): Promise<Record<string, unknown> | null> {
  try {
    const result = await fetchLocateBlogPost(id);
    if (result.success && result.data) {
      return {
        blogPost: (result.data as LocateBlogPostQuery).blogQueries
          .locateBlogPost,
      };
    }
    return null;
  } catch (error) {
    console.error("Failed to fetch blog post data:", error);
    return null;
  }
}

/**
 * Register the data loader.
 */
export function registerBlogPageDataLoader(): void {
  pageDataRegistry.registerPageDataLoader("blog/:id", async (params) => {
    const id = params?.id as string;
    if (!id || id == "create") return null;
    return getBlogPostDetails(id);
  });
}

export default BlogPostPage;
