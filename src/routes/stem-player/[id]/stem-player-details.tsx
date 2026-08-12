import { LocateSongQuery, Song } from "~/generated/graphql";
import StemPlayer from "~/_components/stem-player";
import { usePageData } from "@sun/ssr/react";
import { useParams } from "react-router-dom";
import styles from "./stem-player-details.module.css";

/**
 * Stem Player Details Page.
 */
const StemPlayerDetailsPage = () => {
  const { id } = useParams<{ id: string }>();
  const { data: song } = usePageData<
    LocateSongQuery["stemPlayerQueries"]["locate"]
  >("song", "stem-player/:id", { id });

  if (!song?.path) {
    return null;
  }

  return <StemPlayer className={styles.stemPlayer} song={song as Song} />;
};

export default StemPlayerDetailsPage;
