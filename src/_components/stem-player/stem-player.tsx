import Button from "~/components/button";
import { useStemPlayer } from "./hooks/use-stem-player";
import Label from "~/components/label";
import Input from "~/components/input";
import { ChangeEvent } from "react";
import { formatTime } from "./utils/format-time";
import { formatHoverTime } from "./utils/format-hover-time";
import StemSliders from "./stem-sliders";
import styles from "./stem-player.module.css";
import { useTranslation } from "react-i18next";
import { Stem } from "~/generated/graphql";

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
  const SEEK_BUFFER = 0.1; // this allows us to reach the end of the song with the slider

  const {
    loaded,
    loadingProgress,
    playing,
    ended,
    play,
    stop,
    skip,
    seek,
    position,
    duration,
    masterVolume,
    setVolume,
    setMasterVolume,
  } = useStemPlayer(stems);

  /**
   * Handle seek by setting the audio time to the slider level.
   *
   * @param e Input change event.
   */
  const handleSeek = (e: ChangeEvent<HTMLInputElement>) => {
    const newTime = parseFloat(e.target.value);
    seek(Math.min(newTime, duration));
  };

  /**
   * Handle mouse move on seeker to update title with hover position.
   *
   * @param e Mouse event.
   */
  const handleSeekerMouseMove = (e: React.MouseEvent<HTMLInputElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const percentage = x / rect.width;
    const hoverTime = Math.max(
      0,
      Math.min(duration + SEEK_BUFFER, percentage * (duration + SEEK_BUFFER))
    );
    e.currentTarget.title = t("controls.title.seek", {
      position: formatHoverTime(hoverTime),
    });
  };

  /**
   * Handle mouse move on master volume to update title with hover position
   *
   * @param e Mouse event.
   */
  const handleMasterVolumeMouseMove = (
    e: React.MouseEvent<HTMLInputElement>
  ) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const percentage = x / rect.width;
    const volume = percentage * 2;
    e.currentTarget.title = t("controls.title.master-volume", {
      volume: Math.round(volume * 100),
    });
  };

  // Show loading progress if not fully loaded
  if (!loaded) {
    return (
      <div {...rest} className={`${styles.container} ${rest.className ?? ""}`}>
        <Label>Loading: {Math.round(loadingProgress)}%</Label>
      </div>
    );
  }

  return (
    <div {...rest} className={`${styles.container} ${rest.className ?? ""}`}>
      <StemSliders stems={stems} setVolume={setVolume} />
      <div className={styles.controls}>
        <Label htmlFor="master-volume">{t("controls.master")}</Label>
        <Input
          id="master-volume"
          type="range"
          min={0}
          max={2}
          step={0.01}
          value={masterVolume}
          onChange={(e) => setMasterVolume(parseFloat(e.target.value))}
          onMouseMove={handleMasterVolumeMouseMove}
          className={styles.seeker}
          aria-label={t("controls.aria.master-volume")}
        />
      </div>
      <div className={styles.controls}>
        <Button
          onClick={playing ? stop : play}
          aria-label={
            playing
              ? t("controls.aria.stop")
              : ended
                ? t("controls.aria.restart")
                : t("controls.aria.play")
          }
          title={
            playing
              ? t("controls.title.stop")
              : ended
                ? t("controls.title.restart")
                : t("controls.title.play")
          }
          aria-pressed={playing}
        >
          {playing
            ? t("controls.stop")
            : ended
              ? t("controls.restart")
              : t("controls.play")}
        </Button>
        {/* Time/seeking */}
        <Button
          variant="secondary"
          onClick={() => skip(-SKIP_OFFSET)}
          aria-label={t("controls.aria.seek-back")}
          title={t("controls.title.seek-back", { offset: SKIP_OFFSET })}
        >
          <strong>{"<<"}</strong>
        </Button>
        <Button
          variant="secondary"
          onClick={() => skip(SKIP_OFFSET)}
          aria-label={t("controls.aria.seek-forward")}
          title={t("controls.title.seek-forward", { offset: SKIP_OFFSET })}
        >
          <strong>{">>"}</strong>
        </Button>
        <Label
          htmlFor="seeker"
          className={styles.timeLabel}
          aria-label={t("playback")}
        >
          {formatTime(position)} / {formatTime(duration)}
        </Label>
        <Input
          id="seeker"
          type="range"
          min={0}
          max={duration + SEEK_BUFFER}
          step={0.1}
          value={position}
          onChange={handleSeek}
          onMouseMove={handleSeekerMouseMove}
          className={styles.seeker}
          aria-label={t("controls.aria.seek")}
        />
      </div>
    </div>
  );
};

export default StemPlayer;
