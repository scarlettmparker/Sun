/**
 * Registers all page data loaders.
 * TODO: Have this as a point of registering data loaders and have specific
 * stem-player etc. data loaders registered here.
 */

import { pageDataRegistry } from "./page-data";
import { fetchList } from "./api";
import { ListSongsQuery } from "~/generated/graphql";

// Register all loaders
pageDataRegistry.registerPageDataLoader("stem-player", async () => {
  const result = await fetchList();
  if (result.success && result.data) {
    return {
      songs: (result.data as ListSongsQuery).stemPlayerQueries.list,
    };
  }
  return null;
});
