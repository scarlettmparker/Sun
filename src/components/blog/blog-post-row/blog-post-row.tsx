import { Link, useLocation } from "react-router-dom";
import { cn } from "@sun/components";
import type { BlogPost } from "~/generated/graphql";
import styles from "./blog-post-row.module.css";

type BlogPostRowProps = {
  /**
   * Post to render.
   */
  post: BlogPost;
};

/**
 * Renders a single blog post as a list row.
 */
const BlogPostRow = (props: BlogPostRowProps) => {
  const { post } = props;
  const { pathname, search } = useLocation();
  const isActive = pathname === `/blog/${post.id}`;

  return (
    <Link to={`/blog/${post.id}${search}`} className={cn(styles.item, isActive && styles.item_active)}>
      <span className={styles.item_title}>{post.title}</span>
    </Link>
  );
};

export default BlogPostRow;
