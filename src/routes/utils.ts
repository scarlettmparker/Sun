// Shared data fetchers

import { ListGalleryItemsByRemoteObjectsQuery } from "~/generated/graphql";
import { fetchListGalleryItemsByRemoteObjects } from "~/utils/api";

/**
 * Data fetching function for gallery items by foreign object. Used in multiple routes.
 * @param ids Foreign IDs for input.
 * @returns Promise resolving to page data or null if no data.
 */
export async function getGalleryItemsByRemoteObjects(
  ids: string[],
): Promise<Record<string, unknown> | null> {
  try {
    const result = await fetchListGalleryItemsByRemoteObjects(ids);
    if (result.success && result.data) {
      return {
        galleryItems: (result.data as ListGalleryItemsByRemoteObjectsQuery)
          .galleryQueries.listByRemoteObjects,
      };
    }
    return {
      error: result.error || "Failed to fetch gallery items",
    };
  } catch (error) {
    console.error("Failed to fetch gallery items by foreign object:", error);
    return {
      error: "An error occurred while fetching data",
    };
  }
}
