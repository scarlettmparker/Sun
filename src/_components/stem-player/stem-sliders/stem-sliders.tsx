import { memo, useCallback, useEffect, useState, useRef } from "react";
import Input from "~/components/input";
import Label from "~/components/label";
import styles from "./stem-sliders.module.css";
import { Stem } from "~/generated/graphql";

type StemControlsProps = {
  /**
   * List of stems.
   */
  stems?: Stem[] | null;

  /**
   * Callback function to set the volume of the stem.
   */
  setVolume: (index: number, value: number) => void;
};

/**
 * Props for the Slider component.
 */
type SliderProps = {
  /**
   * Stem data.
   */
  stem: Stem;

  /**
   * Current value of the slider.
   */
  value: number;

  /**
   * Index of the slider.
   */
  i: number;

  /**
   * Callback for pointer down event.
   */
  onPointerDown: (i: number, e: React.PointerEvent<HTMLDivElement>) => void;

  /**
   * Callback for pointer enter event.
   */
  onPointerEnter: (i: number) => void;

  /**
   * Callback for pointer leave event.
   */
  onPointerLeave: (i: number) => void;

  /**
   * Callback to set the overlay ref.
   */
  setOverlayRef: (i: number, el: HTMLDivElement | null) => void;
};

/**
 * Clamp value between 0 and 1.
 */
function clamp(value: number): number {
  return Math.min(1, Math.max(0, value));
}

/**
 * Convert vertical pointer position to slider value.
 * Top = 1, Bottom = 0 (matches vertical RTL range).
 * Includes buffer zone for easier dragging to extremes.
 */
function valueFromPointerY(y: number, rect: DOMRect): number {
  const buffer = 32;
  const visualTop = rect.top + buffer;
  const visualBottom = rect.bottom - buffer;
  if (y <= visualTop) return 1;
  if (y >= visualBottom) return 0;
  return clamp((visualBottom - y) / (visualBottom - visualTop));
}

/**
 * An individual slider.
 */
const Slider = memo(
  (props: SliderProps) => {
    const {
      stem,
      value,
      i,
      onPointerDown,
      onPointerEnter,
      onPointerLeave,
      setOverlayRef,
    } = props;
    return (
      <div className={styles.slider}>
        <Input
          type="range"
          orient="vertical"
          min={0}
          max={1}
          step={0.01}
          value={value}
          className={styles.range}
          readOnly
        />
        <div
          ref={(el) => setOverlayRef(i, el)}
          className={styles.overlay}
          aria-label={stem.name ?? "stem"}
          onPointerDown={(e) => onPointerDown(i, e)}
          onPointerEnter={() => onPointerEnter(i)}
          onPointerLeave={() => onPointerLeave(i)}
        />
        <Label>{stem.name}</Label>
      </div>
    );
  },
  (prev, next) => prev.value === next.value && prev.stem === next.stem
);

Slider.displayName = "Slider";

/**
 * Rendering stem controls (labels and volume sliders).
 */
const StemSliders = ({ stems, setVolume }: StemControlsProps) => {
  const [values, setValues] = useState<number[]>(
    () => stems?.map(() => 1) ?? []
  );
  const isDown = useRef(false);
  const activeIndex = useRef<number | null>(null);
  const lastValue = useRef(1);
  const overlayRefs = useRef<(HTMLDivElement | null)[]>([]);

  /**
   * Handles pointer down on slider overlay.
   * Immediately sets the slider value.
   */
  const handlePointerDown = useCallback(
    (i: number, e: React.PointerEvent<HTMLDivElement>) => {
      e.preventDefault();
      isDown.current = true;
      activeIndex.current = i;

      const overlay = overlayRefs.current[i];
      if (!overlay) return;
      const rect = overlay.getBoundingClientRect();
      const value = valueFromPointerY(e.clientY, rect);
      lastValue.current = value;

      setValues((prev) => {
        const next = [...prev];
        next[i] = value;
        return next;
      });

      setVolume(i, value);
    },
    [setVolume]
  );

  /**
   * Handles pointer enter event.
   */
  const handlePointerEnter = useCallback((i: number) => {
    if (isDown.current) activeIndex.current = i;
  }, []);

  /**
   * Handles pointer leave event.
   */
  const handlePointerLeave = useCallback((i: number) => {
    if (activeIndex.current === i) activeIndex.current = null;
  }, []);

  /**
   * Sets the overlay ref for the given index.
   */
  const setOverlayRef = useCallback((i: number, el: HTMLDivElement | null) => {
    overlayRefs.current[i] = el;
  }, []);

  const onPointerMove = useCallback(
    (e: PointerEvent): void => {
      if (!isDown.current || activeIndex.current === null) return;

      const overlay = overlayRefs.current[activeIndex.current];
      if (!overlay) return;

      const rect = overlay.getBoundingClientRect();
      const raw = valueFromPointerY(e.clientY, rect);
      const value = e.shiftKey ? lastValue.current : raw;

      lastValue.current = value;

      setValues((prev) => {
        const next = [...prev];
        next[activeIndex.current!] = value;
        return next;
      });

      setVolume(activeIndex.current, value);
    },
    [setVolume]
  );

  function onPointerUp(): void {
    isDown.current = false;
    activeIndex.current = null;
    lastValue.current = 1;
  }

  useEffect(() => {
    window.addEventListener("pointermove", onPointerMove);
    window.addEventListener("pointerup", onPointerUp);

    return () => {
      window.removeEventListener("pointermove", onPointerMove);
      window.removeEventListener("pointerup", onPointerUp);
    };
  }, [setVolume]);

  return (
    <div className={styles.container}>
      {stems?.map((stem, i) => (
        <Slider
          key={i}
          stem={stem}
          value={values[i] ?? 1}
          i={i}
          onPointerDown={handlePointerDown}
          onPointerEnter={handlePointerEnter}
          onPointerLeave={handlePointerLeave}
          setOverlayRef={setOverlayRef}
        />
      ))}
    </div>
  );
};

StemSliders.displayName = "StemSliders";
export default StemSliders;
