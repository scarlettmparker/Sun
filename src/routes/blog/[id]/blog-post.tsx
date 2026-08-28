import { Suspense } from "react";
import { Skeleton } from "@sun/components";
import BlogDetail from "~/components/blog/blog-detail";

/**
 * Blog post detail page.
 */
const BlogPostPage = () => {
  return (
    <Suspense fallback={<Skeleton />}>
      <BlogDetail />
    </Suspense>
  );
};

export default BlogPostPage;
