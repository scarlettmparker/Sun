import { memo } from "react";
import Input from "~/components/input";
import Label from "~/components/label";
import { Stem } from "~/_components/stem-player/types/stem";
import styles from "./stem-sliders.module.css";

type StemControlsProps = {
  /**
   * List of stems.
   */
  stems: Stem[];

  /**
   * Callback function to set the volume of the stem.
   */
  setVolume: (index: number, value: number) => void;
};

/**
 * Rendering stem controls (labels and volume sliders).
 */
const StemSliders = memo((props: StemControlsProps) => {
  const { stems, setVolume } = props;

  return (
    <div className={styles.container}>
      {stems.map((stem, i) => (
        <div key={i} className={styles.slider}>
          <Input
            type="range"
            orient="vertical"
            min={0}
            max={1}
            step={0.01}
            defaultValue={1}
            onChange={(e) => setVolume(i, parseFloat(e.target.value))}
          />
          <Label>{stem.name}</Label>
        </div>
      ))}
    </div>
  );
});

StemSliders.displayName = "StemSliders";
export default StemSliders;
