// Shared data fetchers

import { ListGalleryItemsByForeignObjectsQuery } from "~/generated/graphql";
import { fetchListGalleryItemsByForeignObjects } from "~/utils/api";

/**
 * Data fetching function for gallery items by foreign object. Used in multiple routes.
 * @param ids Foreign IDs for input.
 * @returns Promise resolving to page data or null if no data.
 */
export async function getGalleryItemsByForeignObjects(
  ids: string[]
): Promise<Record<string, unknown> | null> {
  try {
    const result = await fetchListGalleryItemsByForeignObjects(ids);
    if (result.success && result.data) {
      return {
        galleryItems: (result.data as ListGalleryItemsByForeignObjectsQuery)
          .galleryQueries.listByForeignObjects,
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
