import { defineLoader } from "@sun/ssr";
import { executeDocument } from "@sun/api";
import {
  LocateBlogPostDocument,
  LocateReaderTextsDocument,
  type LocateBlogPostQuery,
  type LocateReaderTextsQuery,
} from "~/generated/graphql";

/**
 * Loads attached reader texts for a blog post via batch locate.
 */
defineLoader({
  pattern: "blog/:id/attachedTexts",
  async loader(params) {
    const id = typeof params.id === "string" ? params.id : "";
    if (!id) {
      return { attachedTexts: [] as NonNullable<LocateReaderTextsQuery["hadesQueries"]["locateReaderTexts"]> };
    }
    try {
      const postResult = await executeDocument<LocateBlogPostQuery>(LocateBlogPostDocument, { id });
      const remoteObject = postResult.success
        ? (postResult.data as LocateBlogPostQuery | undefined)?.blogQueries?.locateBlogPost?.remoteObject ?? []
        : [];
      const ids = (remoteObject as string[])
        .filter((value) => value.startsWith("hades:text:"))
        .map((value) => value.replace("hades:text:", ""));
      if (!ids.length) {
        return { attachedTexts: [] as NonNullable<LocateReaderTextsQuery["hadesQueries"]["locateReaderTexts"]> };
      }
      const result = await executeDocument<LocateReaderTextsQuery>(LocateReaderTextsDocument, { ids });
      const attachedTexts = result.success
        ? (result.data as LocateReaderTextsQuery | undefined)?.hadesQueries?.locateReaderTexts ?? []
        : [];
      return { attachedTexts: attachedTexts ?? [] };
    } catch {
      return { attachedTexts: [] as NonNullable<LocateReaderTextsQuery["hadesQueries"]["locateReaderTexts"]> };
    }
  },
});
