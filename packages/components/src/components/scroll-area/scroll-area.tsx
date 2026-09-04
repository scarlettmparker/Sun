import { useCallback, useLayoutEffect, useMemo, useRef, useState } from "react";
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
const ScrollArea = (props: ScrollAreaProps) => {
  const { maxHeight, className, children, ...rest } = props;
  const innerRef = useRef<HTMLDivElement>(null);
  const thumbRef = useRef<HTMLDivElement>(null);
  const [scrollHeight, setScrollHeight] = useState(0);
  const [clientHeight, setClientHeight] = useState(0);
  const dragging = useRef(false);
  const rafRef = useRef<number | null>(null);

  const innerStyle = useMemo(() => (maxHeight ? { maxHeight } : undefined), [maxHeight]);

  /**
   * Reads dimensions without touching scrollTop to avoid re-renders on scroll.
   */
  const measure = useCallback(() => {
    const el = innerRef.current;
    if (el == null) return;
    setScrollHeight(el.scrollHeight);
    setClientHeight(el.clientHeight);
  }, []);

  useLayoutEffect(() => {
    measure();
  }, [measure]);

  /**
   * Measure on resize and when the inner container's content changes.
   */
  useLayoutEffect(() => {
    const el = innerRef.current;
    if (el == null) return;

    const ro = new ResizeObserver(() => {
      requestAnimationFrame(measure);
    });
    ro.observe(el);
    if (el.firstElementChild) {
      ro.observe(el.firstElementChild as Element);
    }

    const onResize = () => requestAnimationFrame(measure);
    window.addEventListener("resize", onResize);

    return () => {
      ro.disconnect();
      window.removeEventListener("resize", onResize);
    };
  }, [measure]);

  const thumbVisible = scrollHeight > clientHeight;

  const thumbHeight = useMemo(() => {
    if (!thumbVisible) return 0;
    return (clientHeight / scrollHeight) * clientHeight;
  }, [thumbVisible, clientHeight, scrollHeight]);

  /**
   * Syncs thumb position directly via DOM without React state.
   */
  const syncThumbPosition = useCallback(() => {
    const el = innerRef.current;
    const thumb = thumbRef.current;
    if (el == null || thumb == null) return;
    const sh = el.scrollHeight;
    const ch = el.clientHeight;
    if (sh <= ch) return;
    const th = (ch / sh) * ch;
    const maxThumbTop = ch - th;
    const scrollable = sh - ch;
    if (maxThumbTop <= 0 || scrollable <= 0) return;
    const top = (el.scrollTop / scrollable) * maxThumbTop;
    thumb.style.top = `${top}px`;
  }, []);

  useLayoutEffect(() => {
    syncThumbPosition();
  }, [thumbHeight, thumbVisible, syncThumbPosition]);

  useLayoutEffect(() => {
    return () => {
      if (rafRef.current != null) cancelAnimationFrame(rafRef.current);
    };
  }, []);

  /**
   * Updates thumb position on scroll without triggering React render.
   */
  const handleScroll = useCallback(() => {
    if (rafRef.current != null) cancelAnimationFrame(rafRef.current);
    rafRef.current = requestAnimationFrame(() => {
      syncThumbPosition();
    });
  }, [syncThumbPosition]);

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
        const el = innerRef.current;
        if (!dragging.current || el == null) return;
        const delta = ev.clientY - startY;
        const sh = el.scrollHeight;
        const ch = el.clientHeight;
        const th = (ch / sh) * ch;
        const scrollable = sh - ch;
        const dragRange = ch - th;
        if (dragRange <= 0) return;
        const dragRatio = scrollable / dragRange;
        el.scrollTop = startScrollTop + delta * dragRatio;
      };

      const onUp = () => {
        dragging.current = false;
        document.removeEventListener("mousemove", onMove);
        document.removeEventListener("mouseup", onUp);
      };

      document.addEventListener("mousemove", onMove);
      document.addEventListener("mouseup", onUp);
    },
    [],
  );

  return (
    <div className={cn(styles.outer, className)} {...rest}>
      <div ref={innerRef} className={styles.inner} style={innerStyle} onScroll={handleScroll}>
        {children}
      </div>
      {thumbVisible ? (
        <div className={styles.track}>
          <div
            ref={thumbRef}
            className={styles.thumb}
            style={{ height: thumbHeight, top: 0 }}
            onMouseDown={handleThumbMouseDown}
          />
        </div>
      ) : null}
    </div>
  );
};

export default ScrollArea;
