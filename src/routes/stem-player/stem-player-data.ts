import { defineLoader } from "@sun/ssr";
import { fetchListSongs } from "~/utils/api";
import type { ListSongsQuery } from "~/generated/graphql";

/**
 * Loads the song list into the page-data cache.
 */
defineLoader({
  pattern: "stem-player",
  async loader() {
    try {
      const result = await fetchListSongs();
      const songs = result.success
        ? (result.data as ListSongsQuery | undefined)?.stemPlayerQueries?.list
        : null;
      return { songs: songs ?? [] };
    } catch {
      return { songs: [] };
    }
  },
});
