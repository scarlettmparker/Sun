import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import { MeDocument, type MeQuery } from "~/generated/graphql";

/**
 * Loads the current account for the home profile card.
 */
defineLoader({
  pattern: "currentUser",
  async loader() {
    try {
      const result = await executeDocument<MeQuery>(MeDocument, {});
      const me = result.success
        ? (result.data as MeQuery | undefined)?.gaiaQueries.me ?? null
        : null;
      return { currentUser: me };
    } catch {
      return { currentUser: null as MeQuery["gaiaQueries"]["me"] };
    }
  },
});
