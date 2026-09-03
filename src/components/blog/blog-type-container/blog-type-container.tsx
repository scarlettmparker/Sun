import { Suspense, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import BlogTypeList from "~/components/blog/blog-type-list";
import BlogPostTypeFetcher from "~/components/blog/blog-post-type-fetcher";

type BlogTypeContainerProps = {
  /**
   * Layout variant forwarded to the list.
   */
  variant?: "vertical" | "horizontal";
};

/**
 * Container that provides selected type and navigation for blog type list.
 */
const BlogTypeContainer = (props: BlogTypeContainerProps) => {
  const { variant } = props;
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [postType, setPostType] = useState<string | null>(null);
  const queryType = searchParams.get("type");
  const selectedType = id ? (postType ?? queryType) : queryType;

  const handleSelect = (type: string | null) => {
    if (id) {
      if (type == null) {
        navigate("/blog", { preventScrollReset: true });
      } else {
        navigate(`/blog?type=${encodeURIComponent(type)}`, {
          preventScrollReset: true,
        });
      }
      return;
    }
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (type == null) {
          next.delete("type");
        } else {
          next.set("type", type);
        }
        next.delete("page");
        return next;
      },
      { preventScrollReset: true },
    );
  };

  return (
    <>
      <BlogTypeList
        selectedType={selectedType}
        onSelect={handleSelect}
        variant={variant}
      />
      {id ? (
        <Suspense fallback={null}>
          <BlogPostTypeFetcher postId={id} onTypeChange={setPostType} />
        </Suspense>
      ) : null}
    </>
  );
};

export default BlogTypeContainer;
