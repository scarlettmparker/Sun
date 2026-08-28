import { Suspense } from "react";
import { useSearchParams } from "react-router-dom";
import BlogList from "~/components/blog/blog-list";
import { BlogListSkeleton } from "~/components/blog/skeletons";

/**
 * Blog list outlet page.
 */
const BlogListPage = () => {
  const [searchParams] = useSearchParams();
  const selectedType = searchParams.get("type");

  return (
    <Suspense fallback={<BlogListSkeleton />}>
      <BlogList selectedType={selectedType} />
    </Suspense>
  );
};

export default BlogListPage;
