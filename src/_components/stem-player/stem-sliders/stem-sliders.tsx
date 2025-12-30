import { memo, useEffect, useState, useRef } from "react";
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
   * Callback functio to set the volume of the stem.
   */
  setVolume: (index: number, value: number) => void;
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
 */
function valueFromPointerY(y: number, rect: DOMRect): number {
  return clamp((rect.bottom - y) / rect.height);
}

/**
 * Rendering stem controls (labels and volume sliders).
 */
const StemSliders = memo(({ stems, setVolume }: StemControlsProps) => {
  const [values, setValues] = useState<number[]>(
    () => stems?.map(() => 1) ?? []
  );
  const isDown = useRef(false);
  const activeIndex = useRef<number | null>(null);
  const lastValue = useRef(1);
  const overlayRefs = useRef<(HTMLDivElement | null)[]>([]);

  // Update slider value on pointer move
  useEffect(() => {
    function onPointerMove(e: PointerEvent): void {
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
    }

    function onPointerUp(): void {
      isDown.current = false;
      activeIndex.current = null;
      lastValue.current = 1;
    }

    window.addEventListener("pointermove", onPointerMove);
    window.addEventListener("pointerup", onPointerUp);

    return () => {
      window.removeEventListener("pointermove", onPointerMove);
      window.removeEventListener("pointerup", onPointerUp);
    };
  }, [setVolume]);

  /**
   * Handle pointer down on slider overlay.
   * Immediately sets the slider value.
   */
  const handlePointerDown = (
    i: number,
    e: React.PointerEvent<HTMLDivElement>
  ) => {
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
  };

  return (
    <div className={styles.container}>
      {stems?.map((stem, i) => (
        <div key={i} className={styles.slider}>
          <Input
            type="range"
            orient="vertical"
            min={0}
            max={1}
            step={0.01}
            value={values[i] ?? 1}
            className={styles.range}
            readOnly
          />

          <div
            ref={(el) => {
              overlayRefs.current[i] = el;
            }}
            className={styles.overlay}
            aria-label={stem.name ?? "stem"}
            onPointerDown={(e) => handlePointerDown(i, e)}
            onPointerEnter={() => {
              if (isDown.current) activeIndex.current = i;
            }}
            onPointerLeave={() => {
              if (activeIndex.current === i) activeIndex.current = null;
            }}
          />

          <Label>{stem.name}</Label>
        </div>
      ))}
    </div>
  );
});

StemSliders.displayName = "StemSliders";
export default StemSliders;
