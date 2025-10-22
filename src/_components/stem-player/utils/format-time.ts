/**
 * Format time from seconds to M:SS
 *
 * @param seconds Time in seconds.
 */
export function formatTime(seconds: number): string {
  const isNegative = seconds < 0;

  // get abs to prevent potential odd formatting with negatives
  const absSeconds = Math.abs(seconds);
  const m = Math.floor(absSeconds / 60);
  const s = Math.floor(absSeconds % 60)
    .toString()
    .padStart(2, "0");

  return isNegative ? `-${m}:${s}` : `${m}:${s}`;
}
