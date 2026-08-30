import { MutationResult, executeMutation } from "@sun/ssr";

/**
 * Creates a new blog post.
 * @param title The title of the blog post.
 * @param content The content of the blog post.
 * @param typeId Optional type id.
 * @param parentId Optional parent id.
 * @returns Promise resolving to the mutation result.
 */
export async function createBlogPost(
  title: string,
  content: string,
  typeId?: string,
  parentId?: string,
): Promise<MutationResult> {
  if (
    typeof title !== "string" ||
    typeof content !== "string" ||
    !title.trim() ||
    !content.trim()
  ) {
    return {
      __typename: "StandardError",
      message: "Invalid input: title and content must be non-empty strings",
    };
  }

  const result = await executeMutation("blog/create", {
    title: title.trim(),
    input: {
      content: content.trim(),
      ...(typeId ? { typeId } : {}),
      ...(parentId ? { parentId } : {}),
    },
  });

  if (result.__typename === "Redirect") {
    window.location.assign(result.redirectTo);
  }

  return result;
}

/**
 * Attaches a remote object to a post.
 */
export async function attachRemoteObject(postId: string, target: string): Promise<MutationResult> {
  return executeMutation("blog/add-remote-object", { postId, target });
}

/**
 * Detaches a remote object from a post.
 */
export async function detachRemoteObject(postId: string, target: string): Promise<MutationResult> {
  return executeMutation("blog/remove-remote-object", { postId, target });
}

/**
 * Ingests a blog from a source.
 */
export async function ingestBlogFromSource(input: {
  title: string;
  typeName: string;
  sourceKind: string;
  sourceId: string;
  parentId?: string | null;
}): Promise<MutationResult> {
  const result = await executeMutation("blog/ingest-source", { input });
  if (result.__typename === "Redirect") {
    window.location.assign(result.redirectTo);
  }
  return result;
}
