import { defineLoader } from "@sun/ssr";

/**
 * Resolves the current user's role key strings.
 *
 * TODO: wire to gaiaQueries.myRoles once gaia schema is vendored
 * into Sun's graphql/schemas. For now return empty so RoleCheck
 * gracefully hides admin-only UI.
 */
defineLoader({
  pattern: "currentRoles",
  async loader() {
    return { currentRoles: [] as string[] };
  },
});
