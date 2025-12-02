import { useState } from "react";
import { createBlogPost } from "~/server/actions/blog-post";
import { mutationRegistry } from "~/utils/mutations";

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

    if (result.success) {
      setSuccess(true);
      e.currentTarget.reset();
    } else {
      setError(result.error || "Failed to create blog post");
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

import { mutateCreateBlogPost } from "~/utils/api";

/**
 * Handler for creating a blog post.
 */
async function handleCreateBlogPost(body: Record<string, unknown>) {
  const { title, content } = body;
  if (typeof title !== "string" || typeof content !== "string") {
    return {
      success: false,
      error: "Invalid input: title and content must be strings",
    };
  }
  const result = await mutateCreateBlogPost(title, { content });
  if (result.success) {
    return { success: true };
  } else {
    return {
      success: false,
      error: result.error || "Failed to create blog post",
    };
  }
}

/**
 * Register the mutation handler for blog post creation.
 */
export function registerBlogCreateMutation(): void {
  mutationRegistry.registerMutationHandler("blog/create", handleCreateBlogPost);
}

export default CreateBlogPostPage;
