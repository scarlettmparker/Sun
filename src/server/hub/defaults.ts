import type { HubRegistry } from "./types";

/**
 * Registry used until the stored entry can be read, or when gaia is down.
 */
export const DEFAULT_REGISTRY: HubRegistry = {
  mode: "dev",
  apps: [
    {
      key: "sun",
      name: "Sun",
      dir: ".",
      devPort: 5173,
      prodPort: 5173,
      url: "https://sun.int.scarlettparker.co.uk",
      description: "Ecosystem home with blog and gallery",
      enabled: true,
      self: true,
    },
    {
      key: "guided-reader",
      name: "Guided Reader",
      dir: "../Guided-Reader",
      devPort: 5178,
      prodPort: 5178,
      url: "http://localhost:5178",
      description: "Reading app with texts, annotations and a forum",
      enabled: true,
    },
    {
      key: "checklist",
      name: "Checklist",
      dir: "../Checklist",
      devPort: 5176,
      prodPort: 5176,
      url: "https://checklist.int.scarlettparker.co.uk",
      description: "Structured checklists",
      enabled: true,
    },
    {
      key: "viewer",
      name: "Viewer",
      dir: "../Viewer",
      devPort: 5177,
      prodPort: 5177,
      url: "https://viewer.int.scarlettparker.co.uk",
      description: "Interactive viewer",
      enabled: true,
    },
    {
      key: "mame",
      name: "Emulator",
      dir: "../MAME",
      devPort: 5175,
      prodPort: 5180,
      url: "https://emulator.int.scarlettparker.co.uk",
      description: "Browser MAME emulator",
      enabled: true,
    },
    {
      key: "filestore",
      name: "Filestore",
      dir: "../Filestore",
      devPort: 5174,
      prodPort: 5174,
      url: "https://filestore.int.scarlettparker.co.uk",
      description: "File storage and sharing",
      enabled: true,
    },
  ],
};
