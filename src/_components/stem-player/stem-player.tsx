import Button from "~/components/button";
import { useStemPlayer } from "./hooks/use-stem-player";
import { Stem } from "./types/stem";
import { Fragment } from "react/jsx-runtime";
import Label from "~/components/label";
import Input from "~/components/input";
import { ChangeEvent, useEffect, useState } from "react";
import { formatTime } from "./utils/format-time";

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
  const SKIP_OFFSET = 10;

  // holy context
  const {
    loaded,
    playing,
    play,
    stop,
    skip,
    seek,
    position,
    duration,
    setVolume,
  } = useStemPlayer(stems);

  /**
   * Handle seek by setting the audio time to the slider level.
   *
   * @param e Input change event.
   */
  const handleSeek = (e: ChangeEvent<HTMLInputElement>) => {
    const newTime = parseFloat(e.target.value);
    seek(newTime);
  };

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
        <Button onClick={() => skip(-SKIP_OFFSET)}>{"<<"}</Button>
        <Button onClick={() => skip(SKIP_OFFSET)}>{">>"}</Button>
      </div>
      <div>
        {/* Time/seeking */}
        <Label>
          {formatTime(position)} / {formatTime(duration)}
        </Label>
        <Input
          type="range"
          min={0}
          max={duration}
          step={0.1}
          value={position}
          onChange={handleSeek}
        />
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
