import { Suspense } from "react";
import type { BlogPost } from "~/generated/graphql";
import KnowledgeChildren from "~/components/knowledge/knowledge-children";
import { KnowledgeChildrenSkeleton } from "~/components/knowledge/knowledge-children/skeletons";
import RelatedPosts from "~/components/knowledge/related-posts";
import { RelatedPostsSkeleton } from "~/components/knowledge/related-posts/skeletons";
import RelatedGallery from "~/components/knowledge/related-gallery";
import { RelatedGallerySkeleton } from "~/components/knowledge/related-gallery/skeletons";
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
      <Suspense fallback={<KnowledgeChildrenSkeleton />}>
        <KnowledgeChildren postId={post.id} />
      </Suspense>
      {blogTargets.length > 0 ? (
        <Suspense fallback={<RelatedPostsSkeleton />}>
          <RelatedPosts ids={blogTargets} />
        </Suspense>
      ) : null}
      {galleryTargets.length > 0 ? (
        <Suspense fallback={<RelatedGallerySkeleton />}>
          <RelatedGallery ids={galleryTargets} />
        </Suspense>
      ) : null}
    </div>
  );
};

export default KnowledgeGraph;
