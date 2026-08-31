import { executeDocument } from "@sun/api";
import {
  ListGalleryItemsByRemoteObjectsDocument,
  type ListGalleryItemsByRemoteObjectsQuery,
} from "~/generated/graphql";

/**
 * Data fetching function for gallery items by foreign object. Used in multiple routes.
 *
 * @param ids foreign ids for input
 * @returns promise resolving to page data
 */
export async function getGalleryItemsByRemoteObjects(
  ids: string[],
): Promise<Record<string, unknown> | null> {
  try {
    const result = await executeDocument<ListGalleryItemsByRemoteObjectsQuery>(
      ListGalleryItemsByRemoteObjectsDocument,
      { ids },
    );
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
