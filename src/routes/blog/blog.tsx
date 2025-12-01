import { ListBlogPostsQuery } from "~/generated/graphql";
import { fetchListBlogPosts } from "~/utils/api";
import { pageDataRegistry } from "~/utils/page-data";
import { useTranslation } from "react-i18next";
import styles from "./blog.module.css";
import { groupPostsByMonthYear } from "./group-posts-by-month-year";
import React from "react";

const BlogPage = () => {
  const { t } = useTranslation("blog");

  const pageData = globalThis.__pageData__;
  const initialData =
    pageData?.blogPosts as ListBlogPostsQuery["blogQueries"]["listBlogPosts"];

  if (!initialData) {
    return <>Loading...</>;
  }

  const groupedPosts = groupPostsByMonthYear(initialData);

  return (
    <div className={styles.blog_wrapper}>
      {groupedPosts.map((group) => (
        <React.Fragment key={group.monthYear}>
          <h1>{t(group.monthYear)}</h1>
          <hr />
          {group.posts.map((blogPost) => (
            <h2 key={blogPost.id}>{blogPost.title}</h2>
          ))}
        </React.Fragment>
      ))}
    </div>
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
