import { defineLoader } from "@sun/ssr";
import { fetchListGalleryItems } from "~/utils/api";
import type { ListGalleryItemsQuery } from "~/generated/graphql";

/**
 * Loads the gallery items into the page-data cache.
 */
defineLoader({
  pattern: "gallery",
  async loader() {
    try {
      const result = await fetchListGalleryItems();
      const galleryItems = result.success
        ? (result.data as ListGalleryItemsQuery | undefined)?.galleryQueries
            ?.list
        : null;
      return { galleryItems: galleryItems ?? [] };
    } catch {
      return { galleryItems: [] };
    }
  },
});
