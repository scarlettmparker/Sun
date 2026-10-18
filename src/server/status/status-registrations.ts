import { defineLoader } from "@sun/ssr";
import { buildReport } from "./report";

/**
 * Loads the live status snapshot.
 */
defineLoader({
  pattern: "status",
  async loader() {
    return { status: await buildReport() };
  },
});
