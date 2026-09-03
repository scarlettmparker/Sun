import { defineLoader } from "@sun/ssr";
import { getCookieValue } from "@sun/api";
import { AUTH_COOKIE, getCurrentUser } from "~/utils/auth";

/**
 * Loads the current account for the home profile card.
 */
defineLoader({
  pattern: "currentUser",
  async loader(_params, context) {
    const token = getCookieValue(
      (context as { cookie?: string } | undefined)?.cookie,
      AUTH_COOKIE,
    );
    return { currentUser: await getCurrentUser(token) };
  },
});
