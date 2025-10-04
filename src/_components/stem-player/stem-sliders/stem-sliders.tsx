import { Fragment, memo } from "react";
import Input from "~/components/input";
import Label from "~/components/label";
import { Stem } from "~/_components/stem-player/types/stem";

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

  return stems.map((stem, i) => (
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
  ));
});

export default StemSliders;
