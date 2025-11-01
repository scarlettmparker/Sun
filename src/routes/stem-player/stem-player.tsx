import { fetchList } from "~/utils/api";
import { pageDataRegistry } from "~/utils/page-data";
import { ListSongsQuery } from "~/generated/graphql";
import { Outlet } from "react-router-dom";

/**
 * Stem Player Page component.
 */
const StemPlayerPage = () => {
  const pageData = globalThis.__pageData__;
  const initialData =
    pageData?.songs as ListSongsQuery["stemPlayerQueries"]["list"];

  if (!initialData) {
    return <div>Loading...</div>;
  }

  // // TODO: shouldn't be hard coded like this
  // const fellInAgainSong = initialData?.find(
  //   (song) => song?.name === "Fell In Again"
  // );

  // if (!fellInAgainSong?.stems) {
  //   return <div>Song not found</div>;
  // }

  // const stems: Stem[] = fellInAgainSong.stems
  //   .filter(
  //     (stem): stem is Stem => stem?.filePath != null && stem?.name != null
  //   )
  //   .map((stem) => ({
  //     name: stem.name!,
  //     filePath: `/_components/stem-player/fell-in-again/stems/${stem.filePath}`,
  //   }));

  return (
    <>
      Stem Player
      <Outlet />
    </>
    // <StemPlayer
    //   className={styles.stemPlayer}
    //   stems={stems as unknown as Stem[]}
    // />
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
    const result = await fetchList();
    if (result.success && result.data) {
      return {
        songs: (result.data as ListSongsQuery).stemPlayerQueries.list,
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

export default StemPlayerPage;
