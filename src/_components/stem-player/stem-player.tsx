import Button from "~/components/button";
import { useStemPlayer } from "./hooks/use-stem-player";
import { Stem } from "./types/stem";
import { Fragment } from "react/jsx-runtime";
import Label from "~/components/label";
import Input from "~/components/input";

type StemPlayerProps = {
  /**
   * List of stems.
   */
  stems: Stem[];
};

/**
 * Stem player. Plays music from a list of audio files (stems),
 * allowing for control over each track.
 */
const StemPlayer = (props: StemPlayerProps) => {
  const { stems } = props;
  const { loaded, playing, play, stop, setVolume } = useStemPlayer(stems);

  // TODO: fallback pattern
  if (!loaded) return <></>;

  // TODO: styling
  return (
    <>
      <div>
        {playing ? (
          <Button onClick={stop}>Stop</Button>
        ) : (
          <Button onClick={play}>Play</Button>
        )}
      </div>
      <div>
        {stems.map((stem, i) => (
          <Fragment key={i}>
            <Label>{stem.name}</Label>
            <Input
              type="range"
              min={0}
              max={1}
              step={0.01}
              defaultValue={1}
              onChange={(e) => setVolume(i, parseFloat(e.target.value))}
            />
          </Fragment>
        ))}
      </div>
    </>
  );
};

export default StemPlayer;
