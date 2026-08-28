import BlogBreadcrumbInner from "~/components/blog/blog-breadcrumb-inner";
import type { BlogPost } from "~/generated/graphql";

type BlogBreadcrumbProps = {
  /**
   * Blog post for breadcrumb.
   */
  post: BlogPost | null | undefined;
};

/**
 * Breadcrumb for blog post detail.
 */
const BlogBreadcrumb = (props: BlogBreadcrumbProps) => {
  const { post } = props;

  return <BlogBreadcrumbInner post={post} />;
};

export default BlogBreadcrumb;
