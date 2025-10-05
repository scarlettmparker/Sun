import Button from "~/components/button";
import { useStemPlayer } from "./hooks/use-stem-player";
import { Stem } from "./types/stem";
import Label from "~/components/label";
import Input from "~/components/input";
import { ChangeEvent } from "react";
import { formatTime } from "./utils/format-time";
import StemSliders from "./stem-sliders";
import styles from "./stem-player.module.css";
import { useTranslation } from "react-i18next";

type StemPlayerProps = {
  /**
   * List of stems.
   */
  stems: Stem[];
} & React.HTMLAttributes<HTMLDivElement>;

/**
 * Stem player. Plays music from a list of audio files (stems),
 * allowing for control over each track.
 */
const StemPlayer = (props: StemPlayerProps) => {
  const { stems, ...rest } = props;
  const { t } = useTranslation("stem-player");

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
    <div {...rest} className={`${styles.container} ${rest.className ?? ""}`}>
      <StemSliders stems={stems} setVolume={setVolume} />
      <div className={styles.controls}>
        <Button
          onClick={playing ? stop : play}
          arial-label={playing ? t("controls.stop") : t("controls.play")}
          aria-pressed={playing}
        >
          {playing ? t("controls.stop") : t("controls.play")}
        </Button>
        {/* Time/seeking */}
        <Button
          variant="secondary"
          onClick={() => skip(-SKIP_OFFSET)}
          aria-label={t("controls.seek-back", { offset: SKIP_OFFSET })}
        >
          <strong>{"<<"}</strong>
        </Button>
        <Button
          variant="secondary"
          onClick={() => skip(SKIP_OFFSET)}
          aria-label={t("controls.seek-forward", { offset: SKIP_OFFSET })}
        >
          <strong>{">>"}</strong>
        </Button>
        <Label className={styles.timeLabel} aria-label={t("playback")}>
          {formatTime(position)} / {formatTime(duration)}
        </Label>
        <Input
          type="range"
          min={0}
          max={duration}
          step={0.1}
          value={position}
          onChange={handleSeek}
          className={styles.seeker}
          aria-label={t("seek")}
        />
      </div>
    </div>
  );
};

export default StemPlayer;
