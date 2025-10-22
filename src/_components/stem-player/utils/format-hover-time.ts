/**
 * Format time for hover display with appropriate precision based on the time value itself.
 * Shows seconds for <1m, m:ss for <1h, h:mm:ss for >=1h.
 *
 * @param seconds Time in seconds.
 */
export function formatHoverTime(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);

  if (seconds < 60) {
    // less than 1 minute, show seconds
    return `${Math.floor(seconds)}s`;
  } else if (seconds < 3600) {
    // less than 1 hour, m:ss
    return `${minutes}m ${secs}s`;
  } else {
    // 1 hour or more, h:mm:ss
    return `${hours}h ${minutes}m ${secs}s`;
  }
}
