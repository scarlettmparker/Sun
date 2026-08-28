import { Suspense } from "react";
import type { BlogPost } from "~/generated/graphql";
import KnowledgeChildren from "~/components/knowledge/knowledge-children";
import RelatedPosts from "~/components/knowledge/related-posts";
import RelatedGallery from "~/components/knowledge/related-gallery";
import styles from "./knowledge-graph.module.css";

type KnowledgeGraphProps = {
  /**
   * Blog post to render graph for.
   */
  post: BlogPost;
};

/**
 * Renders children tree and related graph edges for a post.
 */
const KnowledgeGraph = (props: KnowledgeGraphProps) => {
  const { post } = props;
  const remoteObject = post.remoteObject ?? [];
  const blogTargets = remoteObject.filter((value) => value.startsWith("briareus:post:"));
  const galleryTargets = remoteObject.filter((value) => value.startsWith("cerberus:gallery:"));

  return (
    <div className={styles.graph_wrapper}>
      <Suspense fallback={null}>
        <KnowledgeChildren postId={post.id} />
      </Suspense>
      <Suspense fallback={null}>
        <RelatedPosts ids={blogTargets} />
      </Suspense>
      <Suspense fallback={null}>
        <RelatedGallery ids={galleryTargets} />
      </Suspense>
    </div>
  );
};

export default KnowledgeGraph;
