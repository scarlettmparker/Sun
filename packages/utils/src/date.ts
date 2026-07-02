/**
 * Pads a number to two digits with a leading zero.
 *
 * @param value The number to pad.
 * @returns The zero-padded string.
 */
function pad(value: number): string {
  return String(value).padStart(2, "0");
}

/**
 * Formats a date to DD/MM/YYYY HH:MM.
 *
 * @param date The date to format. Accepts Date, string, or null.
 * @returns The formatted string, or "-" if the date is null/empty.
 */
export function formatDate(date: Date | string | null | undefined): string {
  if (!date) {
    return "-";
  }
  const dt = typeof date === "string" ? new Date(date) : date;
  if (isNaN(dt.getTime())) {
    return "-";
  }
  return `${pad(dt.getDate())}/${pad(dt.getMonth() + 1)}/${dt.getFullYear()} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
}
