/**
 * Event keys for cross-iframe communication.
 */
export const FILESTORE_EVENTS = {
  FILE_DOWNLOAD: "file:download",
} as const;

/**
 * Event payload types for cross-iframe communication.
 */
export interface FilestoreEventPayloads {
  [key: string]: unknown;
  [FILESTORE_EVENTS.FILE_DOWNLOAD]: { url: string };
}
