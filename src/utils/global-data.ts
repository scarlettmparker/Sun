import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import { getCookieValue } from "@sun/api";
import { AUTH_COOKIE } from "./auth";
import {
  MyRolesDocument,
  type MyRolesQuery,
  PropertySetDocument,
  type PropertySetQuery,
} from "~/generated/graphql";

/**
 * Loads the configurable level-to-colour map from gaia with a short timeout.
 */
async function loadLevelColours(): Promise<Record<string, string>> {
  const result = await executeDocument<PropertySetQuery>(
    PropertySetDocument,
    {
      ownerKey: "ReactApp",
      name: "reader-level-colours",
    },
    undefined,
    { retries: [], timeoutMs: 2000 },
  );
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
 */
defineLoader({
  pattern: "currentRoles",
  async loader(_params, context) {
    const token = getCookieValue(context?.cookie, AUTH_COOKIE);
    if (!token) {
      return { currentRoles: [] as string[] };
    }
    try {
      const res = await executeDocument<MyRolesQuery>(
        MyRolesDocument,
        {},
        token,
        { retries: [], timeoutMs: 2000 },
      );
      return { currentRoles: res.data?.gaiaQueries?.myRoles ?? [] };
    } catch {
      return { currentRoles: [] as string[] };
    }
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
