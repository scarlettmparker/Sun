import { mutationRegistry } from "~/utils/mutations";
import { MutationResult } from "~/server/actions/utils";
import { mutateCreateBlogPost } from "~/utils/api";
import { BlogPostInput } from "~/generated/graphql";
import { invalidateCache } from "~/utils/page-data";
import CreateBlogForm from "~/_components/blog/create/";

const CreateBlogPostPage = () => {
  return <CreateBlogForm />;
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
