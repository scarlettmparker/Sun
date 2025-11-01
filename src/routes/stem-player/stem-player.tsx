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

  return (
    <>
      Stem Player
      <Outlet />
    </>
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
    if (result?.data && result?.success) {
      const songs = (result.data as ListSongsQuery).stemPlayerQueries.list;
      if (songs) {
        return { songs: songs };
      }
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
