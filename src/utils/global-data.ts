import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import {
  PropertySetDocument,
  type PropertySetQuery,
} from "~/generated/graphql";

/**
 * Loads the configurable level-to-colour map from gaia.
 */
async function loadLevelColours(): Promise<Record<string, string>> {
  const result = await executeDocument<PropertySetQuery>(PropertySetDocument, {
    ownerKey: "ReactApp",
    name: "reader-level-colours",
  });
  const entries = (result.data as PropertySetQuery | undefined)?.gaiaQueries
    ?.propertySet as Record<string, { colour?: string }> | undefined;
  if (!entries) {
    return {};
  }
  const colours: Record<string, string> = {};
  for (const [name, value] of Object.entries(entries)) {
    if (value?.colour) {
      colours[name] = value.colour;
    }
  }
  return colours;
}

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

/**
 * Resolves the CEFR level -> colour map.
 */
defineLoader({
  pattern: "levelColours",
  async loader() {
    return { levelColours: await loadLevelColours() };
  },
});
