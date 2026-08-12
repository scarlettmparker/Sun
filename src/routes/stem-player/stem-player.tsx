import { usePageData } from "@sun/ssr/react";
import { ListSongsQuery } from "~/generated/graphql";
import { Outlet } from "react-router-dom";
import { Sidebar } from "@sun/components";
import { Button } from "@sun/components";
import styles from "./stem-player.module.css";
import { Music } from "lucide-react";

/**
 * Stem Player Page component.
 */
const StemPlayerPage = () => {
  const { data: songs } = usePageData<
    ListSongsQuery["stemPlayerQueries"]["list"]
  >("songs", "stem-player");

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

export default StemPlayerPage;
