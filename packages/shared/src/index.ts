// Origins
export const FILESTORE_ORIGIN = "https://filestore.int.scarlettparker.co.uk";
export const MAME_ORIGIN = "https://mame.int.scarlettparker.co.uk";

// Event keys
export const FILESTORE_EVENTS = {
  FILE_DOWNLOAD: "file:download",
} as const;

// Event payload types
export interface FilestoreEventPayloads {
  [key: string]: unknown;
  [FILESTORE_EVENTS.FILE_DOWNLOAD]: { url: string };
}