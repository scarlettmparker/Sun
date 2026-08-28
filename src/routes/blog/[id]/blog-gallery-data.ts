import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import {
  ListGalleryItemsByRemoteObjectsDocument,
  type ListGalleryItemsByRemoteObjectsQuery,
} from "~/generated/graphql";

/**
 * Loads gallery items by remoteObject ids.
 */
defineLoader({
  pattern: "blog/gallery",
  async loader(params) {
    const rawIds = (params as Record<string, unknown>).ids;
    let ids: string[] = [];
    if (Array.isArray(rawIds)) {
      ids = rawIds as string[];
    } else if (typeof rawIds === "string") {
      ids = JSON.parse(rawIds) as string[];
    }
    if (!ids.length) {
      return { galleryItems: [] as NonNullable<ListGalleryItemsByRemoteObjectsQuery["galleryQueries"]["listByRemoteObjects"]> };
    }
    try {
      const result = await executeDocument<ListGalleryItemsByRemoteObjectsQuery>(
        ListGalleryItemsByRemoteObjectsDocument,
        { ids },
      );
      const galleryItems = result.success
        ? (result.data as ListGalleryItemsByRemoteObjectsQuery | undefined)?.galleryQueries?.listByRemoteObjects ?? []
        : [];
      return { galleryItems: galleryItems ?? [] };
    } catch {
      return { galleryItems: [] as NonNullable<ListGalleryItemsByRemoteObjectsQuery["galleryQueries"]["listByRemoteObjects"]> };
    }
  },
});
