import StemPlayer from "~/_components/stem-player";
import { fetchListSongs } from "~/utils/api";
import { ListSongsQuery, Stem as GraphQLStem } from "~/generated/graphql";
import { pageDataRegistry } from "~/utils/page-data";
import styles from "./stem-player.module.css";
import type { Stem } from "~/_components/stem-player/types/stem";

/**
 * Stem Player Page component.
 */
const StemPlayerPage = () => {
  const pageData = typeof window !== "undefined" ? window.__pageData__ : null;
  const initialData =
    pageData?.songs as ListSongsQuery["stemPlayerQueries"]["listSongs"];

  if (!initialData) {
    return <div>Loading...</div>;
  }

  // TODO: shouldn't be hard coded like this
  const fellInAgainSong = initialData?.find(
    (song) => song?.name === "Fell In Again"
  );

  if (!fellInAgainSong?.stems) {
    return <div>Song not found</div>;
  }

  const stems: Stem[] = fellInAgainSong.stems
    .filter(
      (stem): stem is GraphQLStem =>
        stem?.filePath != null && stem?.name != null
    )
    .map((stem) => ({
      name: stem.name!,
      url: `/_components/stem-player/fell-in-again/stems/${stem.filePath}`,
    }));

  return (
    <StemPlayer
      className={styles.stemPlayer}
      stems={stems as unknown as GraphQLStem[]}
    />
  );
};

/**
 * Server-side data fetching function for StemPlayerPage.
 */
export async function getStemPlayerData(): Promise<Record<
  string,
  unknown
> | null> {
  try {
    const result = await fetchListSongs();
    if (result.success && result.data) {
      return {
        songs: (result.data as ListSongsQuery).stemPlayerQueries.listSongs,
      };
    }
    return null;
  } catch (error) {
    console.error("Failed to fetch stem player data:", error);
    return null;
  }
}

/**
 * Register the data loader for this page.
 */
export function registerStemPlayerDataLoader(): void {
  pageDataRegistry.registerPageDataLoader("stem-player", getStemPlayerData);
}

registerStemPlayerDataLoader();

export default StemPlayerPage;
