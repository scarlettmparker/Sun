import { Suspense } from "react";
import BlogDetail from "~/components/blog/blog-detail";
import { BlogDetailSkeleton } from "~/components/blog/blog-detail/skeletons";

/**
 * Blog post detail page.
 */
const BlogPostPage = () => {
  return (
    <Suspense fallback={<BlogDetailSkeleton />}>
      <BlogDetail />
    </Suspense>
  );
};

export default BlogPostPage;
