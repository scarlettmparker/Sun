import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import {
  ListGalleryItemsByRemoteObjectsDocument,
  type ListGalleryItemsByRemoteObjectsQuery,
} from "~/generated/graphql";

/**
 * Loads gallery items by remoteObject ids, merging both directions.
 */
defineLoader({
  pattern: "blog/gallery",
  async loader(params) {
    const rawIds = (params as Record<string, unknown>).ids;
    const rawPostId = (params as Record<string, unknown>).postId;
    let ids: string[] = [];
    if (Array.isArray(rawIds)) {
      ids = rawIds as string[];
    } else if (typeof rawIds === "string") {
      try {
        ids = JSON.parse(rawIds) as string[];
      } catch {
        ids = [];
      }
    }
    const postId = typeof rawPostId === "string" ? rawPostId : null;
    const inverseIds = postId ? [`briareus:post:${postId}`] : [];
    const allIds = [...ids, ...inverseIds];
    if (!allIds.length) {
      return {
        galleryItems: [] as NonNullable<
          ListGalleryItemsByRemoteObjectsQuery["galleryQueries"]["listByRemoteObjects"]
        >,
      };
    }
    try {
      const seen = new Set<string>();
      const merged: NonNullable<
        ListGalleryItemsByRemoteObjectsQuery["galleryQueries"]["listByRemoteObjects"]
      > = [];
      const queries: string[][] = [];
      if (ids.length) {
        queries.push(ids);
      }
      if (inverseIds.length) {
        queries.push(inverseIds);
      }
      for (const queryIds of queries) {
        const result =
          await executeDocument<ListGalleryItemsByRemoteObjectsQuery>(
            ListGalleryItemsByRemoteObjectsDocument,
            { ids: queryIds },
          );
        const items = result.success
          ? ((result.data as ListGalleryItemsByRemoteObjectsQuery | undefined)
              ?.galleryQueries?.listByRemoteObjects ?? [])
          : [];
        for (const item of items ?? []) {
          if (item == null || seen.has(item.id)) {
            continue;
          }
          seen.add(item.id);
          merged.push(item);
        }
      }
      return { galleryItems: merged };
    } catch {
      return {
        galleryItems: [] as NonNullable<
          ListGalleryItemsByRemoteObjectsQuery["galleryQueries"]["listByRemoteObjects"]
        >,
      };
    }
  },
});
