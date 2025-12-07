import { fetchListSongs } from "~/utils/api";
import { pageDataRegistry } from "~/utils/page-data";
import { ListSongsQuery } from "~/generated/graphql";
import { Outlet } from "react-router-dom";
import Sidebar from "~/components/sidebar";
import Button from "~/components/button";
import styles from "./stem-player.module.css";
import { Music } from "lucide-react";

/**
 * Stem Player Page component.
 */
const StemPlayerPage = () => {
  const pageData = globalThis.__pageData__;
  const { error } = pageData || {};

  if (error) {
    return <div>Error: {error as string}</div>;
  }

  const songs = pageData?.songs as ListSongsQuery["stemPlayerQueries"]["list"];

  if (!songs) {
    return <>Loading...</>;
  }

  return (
    <>
      <Sidebar>
        <h3 className={styles.header}>Songs</h3>
        {songs?.map((song, idx) => (
          <a
            key={idx}
            href={`/stem-player/${song?.id}`}
            className={styles.songLink}
          >
            <Button variant="secondary" className={styles.songButton}>
              <Music width={16} height={16} />
              {song?.name}
            </Button>
          </a>
        ))}
      </Sidebar>
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
    const result = await fetchListSongs();
    if (result?.data && result?.success) {
      const songs = (result.data as ListSongsQuery).stemPlayerQueries.list;
      if (songs) {
        return { songs: songs };
      }
    }
    return {
      error: result.error || "Failed to fetch song data",
    };
  } catch (error) {
    console.error("Failed to fetch stem player details data:", error);
    return {
      error: "An error occurred while fetching data",
    };
  }
}

/**
 * Register the data loader for this page.
 */
export function registerStemPlayerDataLoader(): void {
  pageDataRegistry.registerPageDataLoader("stem-player", getStemPlayerData);
}

export default StemPlayerPage;
