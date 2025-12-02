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
      success: false,
      error: "Invalid input: title and content must be non-empty strings",
    };
  }

  return executeMutation("blog/create", {
    title: title.trim(),
    content: content.trim(),
  });
}
