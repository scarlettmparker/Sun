import { ListBlogPostsQuery } from "~/generated/graphql";
import { usePageData } from "@sun/ssr/react";
import { useTranslation } from "react-i18next";
import styles from "./blog.module.css";
import { groupPostsByMonthYear } from "./group-posts-by-month-year";
import React from "react";
import { Card, CardBody } from "@sun/components";

const BlogPage = () => {
  const { t } = useTranslation("blog");

  const { data: initialData } = usePageData<
    ListBlogPostsQuery["blogQueries"]["listBlogPosts"]
  >("blogPosts", "blog");

  const groupedPosts = groupPostsByMonthYear(initialData ?? []);

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

export default BlogPage;
