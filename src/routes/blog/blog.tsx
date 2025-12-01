import { ListBlogPostsQuery } from "~/generated/graphql";
import { fetchListBlogPosts } from "~/utils/api";
import { pageDataRegistry } from "~/utils/page-data";

const BlogPage = () => {
  const pageData = globalThis.__pageData__;
  const initialData =
    pageData?.blogPosts as ListBlogPostsQuery["blogQueries"]["listBlogPosts"];

  console.log(initialData);
  if (!initialData) {
    return <>Loading...</>;
  }

  return (
    <>
      {initialData.map((blogPost, idx) => (
        <h1 key={idx}>{blogPost?.title}</h1>
      ))}
    </>
  );
};

/**
 * Server-side data fetching function for Blogsite.
 */
async function getBlogData(): Promise<Record<string, unknown> | null> {
  try {
    console.log("reaching here");
    const result = await fetchListBlogPosts();
    console.log("result", result);
    if (result?.data && result.success) {
      const blogPosts = (result.data as ListBlogPostsQuery).blogQueries
        .listBlogPosts;
      if (blogPosts) {
        return { blogPosts: blogPosts };
      }
    }
    return null;
  } catch (error) {
    console.error("Failed to fetch blog posts:", error);
    return null;
  }
}

/**
 * Register the data loader for this page.
 */
export function registerBlogDataLoader(): void {
  pageDataRegistry.registerPageDataLoader("blog", getBlogData);
}

export default BlogPage;
