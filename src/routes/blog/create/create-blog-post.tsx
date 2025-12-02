import { useState } from "react";
import { createBlogPost } from "~/server/actions/blog-post";
import { mutationRegistry } from "~/utils/mutations";
import { MutationResult } from "~/server/actions/utils";
import { mutateCreateBlogPost } from "~/utils/api";
import { BlogPostInput } from "~/generated/graphql";
import { invalidateCache } from "~/utils/page-data";

const CreateBlogPostPage = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    const formData = new FormData(e.currentTarget);
    const title = formData.get("title") as string;
    const content = formData.get("content") as string;

    const result = await createBlogPost(title, content);

    if (result.__typename === "QuerySuccess") {
      setSuccess(true);
    } else if (result.__typename === "StandardError") {
      setError(result.message);
    }

    setLoading(false);
  };

  return (
    <form onSubmit={handleSubmit}>
      <input type="text" name="title" placeholder="Title" required />
      <textarea name="content" placeholder="Content" required />
      <button type="submit" disabled={loading}>
        {loading ? "Submitting..." : "Submit"}
      </button>
      {error && <p style={{ color: "red" }}>{error}</p>}
      {success && (
        <p style={{ color: "green" }}>Blog post created successfully!</p>
      )}
    </form>
  );
};

/**
 * Handler for creating a blog post.
 */
async function handleCreateBlogPost(
  body: Record<string, unknown>
): Promise<MutationResult> {
  const { title, input } = body;
  const content = (input as BlogPostInput)?.content;
  if (typeof title !== "string" || typeof content !== "string") {
    return {
      __typename: "StandardError" as const,
      message: "Invalid input: title and content must be strings",
    };
  }

  const result = await mutateCreateBlogPost(title, input as BlogPostInput);
  if (result.success) {
    invalidateCache("blog");
    return result.data?.blogMutations.createBlogPost as MutationResult;
  } else {
    return {
      __typename: "StandardError",
      message: result.error || "Failed to create blog post",
    } as const;
  }
}

/**
 * Register the mutation handler for blog post creation.
 */
export function registerBlogCreateMutation(): void {
  mutationRegistry.registerMutationHandler("blog/create", handleCreateBlogPost);
}

export default CreateBlogPostPage;
