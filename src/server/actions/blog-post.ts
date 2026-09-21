import { executeMutation } from "@sun/ssr";
import type {
  AddRemoteObjectResponse,
  CreateBlogPostResponse,
  DeleteBlogPostResponse,
  IngestBlogFromSourceResponse,
  RemoveRemoteObjectResponse,
} from "~/generated/graphql";

/**
 * Creates a new blog post and navigates to it.
 */
export async function createBlogPost(
  title: string,
  content: string,
  typeId?: string,
  parentId?: string,
): Promise<CreateBlogPostResponse> {
  if (
    typeof title !== "string" ||
    typeof content !== "string" ||
    !title.trim() ||
    !content.trim()
  ) {
    throw new Error("Invalid input: title and content must be non-empty strings");
  }

  const response = await executeMutation<CreateBlogPostResponse>("blog/create", {
    title: title.trim(),
    input: {
      content: content.trim(),
      ...(typeId ? { typeId } : {}),
      ...(parentId ? { parentId } : {}),
    },
  });

  window.location.assign(`/blog/${response.post.id}`);
  return response;
}

/**
 * Attaches a remote object to a post.
 */
export async function attachRemoteObject(
  postId: string,
  target: string,
): Promise<AddRemoteObjectResponse> {
  return executeMutation<AddRemoteObjectResponse>("blog/add-remote-object", {
    postId,
    target,
  });
}

/**
 * Detaches a remote object from a post.
 */
export async function detachRemoteObject(
  postId: string,
  target: string,
): Promise<RemoveRemoteObjectResponse> {
  return executeMutation<RemoveRemoteObjectResponse>(
    "blog/remove-remote-object",
    { postId, target },
  );
}

/**
 * Deletes a blog post and its children, then navigates to the blog list.
 */
export async function deleteBlogPost(id: string): Promise<DeleteBlogPostResponse> {
  const response = await executeMutation<DeleteBlogPostResponse>("blog/delete", {
    id,
  });
  window.location.assign("/blog");
  return response;
}

/**
 * Ingests a blog from a source and navigates to it.
 */
export async function ingestBlogFromSource(input: {
  title: string;
  typeName: string;
  sourceKind: string;
  sourceId: string;
  parentId?: string | null;
}): Promise<IngestBlogFromSourceResponse> {
  const response = await executeMutation<IngestBlogFromSourceResponse>(
    "blog/ingest-source",
    { input },
  );
  window.location.assign(`/blog/${response.post.id}`);
  return response;
}
