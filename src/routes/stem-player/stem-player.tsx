import StemPlayer from "~/_components/stem-player";

/**
 * Stem Player Page
 */
const StemPlayerPage = () => {
  // TODO: hard coded to fell in again
  // and also load it from a folder or sumth
  const basePath = "/_components/stem-player/fell-in-again/stems";

  return (
    <StemPlayer
      stems={[
        { name: "Ambience", url: `${basePath}/ambience.mp3` },
        { name: "Bass", url: `${basePath}/bass.mp3` },
        { name: "Drums", url: `${basePath}/drums.mp3` },
        { name: "Flute", url: `${basePath}/flute.mp3` },
        { name: "Percussion", url: `${basePath}/percussion.mp3` },
        { name: "Piano", url: `${basePath}/piano.mp3` },
        { name: "Strings", url: `${basePath}/strings.mp3` },
        { name: "Synths", url: `${basePath}/synths.mp3` },
      ]}
    />
  );
};

export default StemPlayerPage;
