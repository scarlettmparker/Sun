import { useEffect } from "react";
import { usePageData } from "@sun/ssr/react";
import type { LocateBlogPostQuery } from "~/generated/graphql";

type BlogPostTypeFetcherProps = {
  /**
   * Post id to fetch type for.
   */
  postId: string | null;
  /**
   * Called when type is resolved.
   */
  onTypeChange: (type: string | null) => void;
};

/**
 * Fetches post type and notifies parent.
 */
const BlogPostTypeFetcher = (props: BlogPostTypeFetcherProps) => {
  const { postId, onTypeChange } = props;
  const { data: post } = usePageData<LocateBlogPostQuery["blogQueries"]["locateBlogPost"]>(
    "blogPost",
    "blog/:id",
    { id: postId ?? "" },
  );

  useEffect(() => {
    if (postId) {
      onTypeChange(post?.type?.name ?? null);
    }
  }, [post, postId, onTypeChange]);

  return null;
};

export default BlogPostTypeFetcher;
