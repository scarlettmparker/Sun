import { defineLoader } from "@sun/ssr";
import { fetchLocateSong } from "~/utils/api";
import type { LocateSongQuery, Song } from "~/generated/graphql";

const EMPTY_SONG: Song = { id: "", path: "" };

/**
 * Loads a single song into the page-data cache.
 */
defineLoader({
  pattern: "stem-player/:id",
  async loader(params) {
    const id = typeof params.id === "string" ? params.id : "";
    try {
      const result = await fetchLocateSong(id);
      const song = result.success
        ? (result.data as LocateSongQuery | undefined)?.stemPlayerQueries
            ?.locate
        : null;
      return { song: song ?? EMPTY_SONG };
    } catch {
      return { song: EMPTY_SONG };
    }
  },
});
