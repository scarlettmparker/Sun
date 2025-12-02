import { mutateCreateBlogPost } from "~/utils/api";
import { mutationRegistry } from "~/utils/mutations";

const CreateBlogPostPage = () => {
  return (
    <form action="/blog/create" method="post">
      <input type="text" name="title" placeholder="Title" required />
      <textarea name="content" placeholder="Content" required />
      <button type="submit">Submit</button>
    </form>
  );
};

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
    return { success: true, redirect: "/blog" };
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
