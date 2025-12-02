import { invalidateCache } from "~/utils/page-data";
import { MutationResult, executeMutation } from "./utils";

/**
 * Creates a new blog post.
 * @param title The title of the blog post.
 * @param content The content of the blog post.
 * @returns Promise resolving to the mutation result.
 */
export async function createBlogPost(
  title: string,
  content: string
): Promise<MutationResult> {
  if (
    typeof title !== "string" ||
    typeof content !== "string" ||
    !title.trim() ||
    !content.trim()
  ) {
    return {
      message: "Invalid input: title and content must be non-empty strings",
    };
  }

  const result = await executeMutation("blog/create", {
    title: title.trim(),
    input: {
      content: content.trim(),
    },
  });

  switch (result.__typename) {
    case "QuerySuccess":
      // Invalidate cache on success
      invalidateCache("blog");
      break;
  }

  return result;
}
