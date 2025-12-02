import { LocateBlogPostQuery } from "~/generated/graphql";
import { fetchLocateBlogPost } from "~/utils/api";
import { pageDataRegistry } from "~/utils/page-data";

const BlogPostPage = () => {
  const pageData = globalThis.__pageData__;
  const data =
    pageData?.blogPost as LocateBlogPostQuery["blogQueries"]["locateBlogPost"];

  if (!data) {
    return <div>Loading...</div>;
  }

  return <>{data.content}</>;
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
