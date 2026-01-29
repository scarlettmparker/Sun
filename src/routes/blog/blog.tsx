import { ListBlogPostsQuery } from "~/generated/graphql";
import { fetchListBlogPosts } from "~/utils/api";
import { pageDataRegistry, getPageData } from "~/utils/page-data";
import { useTranslation } from "react-i18next";
import styles from "./blog.module.css";
import { groupPostsByMonthYear } from "./group-posts-by-month-year";
import React from "react";
import Card, { CardBody } from "~/components/card";

const BlogPage = () => {
  const { t } = useTranslation("blog");

  const { data: initialData } = getPageData<
    ListBlogPostsQuery["blogQueries"]["listBlogPosts"]
  >("blogPosts", "blog");

  if (!initialData) {
    return null;
  }

  const groupedPosts = groupPostsByMonthYear(initialData);

  return (
    <div className={styles.blog_wrapper}>
      <Card>
        <CardBody>
          {groupedPosts.map((group) => (
            <React.Fragment key={group.monthYear}>
              <h1>{t(group.monthYear)}</h1>
              <hr />
              {group.posts.map((blogPost) => (
                <a key={blogPost.id} href={`/blog/${blogPost.id}`}>
                  <h4>{blogPost.title}</h4>
                </a>
              ))}
            </React.Fragment>
          ))}
        </CardBody>
      </Card>
    </div>
  );
};

/**
 * Server-side data fetching function for Blogsite.
 */
async function getBlogData(): Promise<Record<string, unknown> | null> {
  try {
    const result = await fetchListBlogPosts();
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
