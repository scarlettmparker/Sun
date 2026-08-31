import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import { ListSongsDocument, type ListSongsQuery } from "~/generated/graphql";

/**
 * Loads the song list into the page-data cache.
 */
defineLoader({
  pattern: "stem-player",
  async loader() {
    try {
      const result = await executeDocument<ListSongsQuery>(
        ListSongsDocument,
        {},
      );
      const songs = result.success
        ? (result.data as ListSongsQuery | undefined)?.stemPlayerQueries?.list
        : null;
      return { songs: songs ?? [] };
    } catch {
      return { songs: [] };
    }
  },
});
