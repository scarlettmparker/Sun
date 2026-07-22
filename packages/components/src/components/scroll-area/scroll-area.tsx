import { useCallback, useLayoutEffect, useRef, useState } from "react";
import { cn } from "~/utils/cn";
import styles from "./scroll-area.module.css";

type ScrollAreaProps = {
  /**
   * Maximum height before the container scrolls, e.g. `"22rem"`.
   */
  maxHeight?: string;
} & React.HTMLAttributes<HTMLDivElement>;

/**
 * Custom-scrollbar container with a thin overlay thumb in the primary colour.
 */
const ScrollArea = ({
  maxHeight,
  className,
  children,
  ...rest
}: ScrollAreaProps) => {
  const innerRef = useRef<HTMLDivElement>(null);
  const [scrollTop, setScrollTop] = useState(0);
  const [scrollHeight, setScrollHeight] = useState(0);
  const [clientHeight, setClientHeight] = useState(0);
  const dragging = useRef(false);

  const innerStyle = maxHeight ? { maxHeight } : undefined;

  /**
   * Reads the current scroll metrics from the inner container.
   */
  const measure = useCallback(() => {
    const el = innerRef.current;
    if (!el) return;
    setScrollTop(el.scrollTop);
    setScrollHeight(el.scrollHeight);
    setClientHeight(el.clientHeight);
  }, []);

  useLayoutEffect(() => {
    measure();
  }, []);

  /**
   * Updates scroll state on user scroll.
   */
  const handleScroll = useCallback(() => {
    measure();
  }, [measure]);

  const thumbVisible = scrollHeight > clientHeight;

  let thumbHeight = 0;
  let thumbTop = 0;
  if (thumbVisible) {
    thumbHeight = (clientHeight / scrollHeight) * clientHeight;
    const trackHeight = clientHeight;
    const maxThumbTop = trackHeight - thumbHeight;
    const scrollable = scrollHeight - clientHeight;
    if (maxThumbTop > 0 && scrollable > 0) {
      thumbTop = (scrollTop / scrollable) * maxThumbTop;
    }
  }

  /**
   * Starts drag-scrolling when the user presses the thumb.
   */
  const handleThumbMouseDown = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      dragging.current = true;
      const startY = e.clientY;
      const startScrollTop = innerRef.current?.scrollTop ?? 0;

      const onMove = (ev: MouseEvent) => {
        if (!dragging.current || !innerRef.current) return;
        const delta = ev.clientY - startY;
        const scrollable = scrollHeight - clientHeight;
        const dragRatio = scrollable / (clientHeight - thumbHeight);
        innerRef.current.scrollTop = startScrollTop + delta * dragRatio;
      };

      const onUp = () => {
        dragging.current = false;
        document.removeEventListener("mousemove", onMove);
        document.removeEventListener("mouseup", onUp);
      };

      document.addEventListener("mousemove", onMove);
      document.addEventListener("mouseup", onUp);
    },
    [scrollHeight, clientHeight, thumbHeight],
  );

  return (
    <div className={cn(styles.outer, className)} {...rest}>
      <div
        ref={innerRef}
        className={styles.inner}
        style={innerStyle}
        onScroll={handleScroll}
      >
        {children}
      </div>
      {thumbVisible && (
        <div className={styles.track}>
          <div
            className={styles.thumb}
            style={{ height: thumbHeight, top: thumbTop }}
            onMouseDown={handleThumbMouseDown}
          />
        </div>
      )}
    </div>
  );
};

export default ScrollArea;
