import { fetchLocateSong } from "~/utils/api";
import { LocateSongQuery, Song } from "~/generated/graphql";
import StemPlayer from "~/_components/stem-player";
import { pageDataRegistry } from "~/utils/page-data";
import styles from "./stem-player-details.module.css";
import { getStemPlayerData } from "../stem-player";
/**
 * Stem Player Details Page.
 */
const StemPlayerDetailsPage = () => {
  const pageData = globalThis.__pageData__;
  const data = pageData?.song as LocateSongQuery["stemPlayerQueries"]["locate"];

  if (!data) {
    return <div>Loading...</div>;
  }

  return <StemPlayer className={styles.stemPlayer} song={data as Song} />;
};

/**
 * Data fetching function for StemPlayerDetailsPage.
 * @param id The song ID from the route.
 * @returns Promise resolving to page data or null if no data.
 */
export async function getStemPlayerDetailsData(
  id: string
): Promise<Record<string, unknown> | null> {
  try {
    const result = await fetchLocateSong(id);
    if (result.success && result.data) {
      return {
        song: (result.data as LocateSongQuery).stemPlayerQueries.locate,
      };
    }
    return null;
  } catch (error) {
    console.error("Failed to fetch stem player details data:", error);
    return null;
  }
}

/**
 * Register the data loader.
 */
export function registerStemPlayerDetailsDataLoader(): void {
  // We want both the list and the locate to be called for this page
  pageDataRegistry.registerPageDataLoader("stem-player/:id", getStemPlayerData);
  pageDataRegistry.registerPageDataLoader("stem-player/:id", async (params) => {
    const id = params?.id as string;
    if (!id) return null;
    return getStemPlayerDetailsData(id);
  });
}

export default StemPlayerDetailsPage;
