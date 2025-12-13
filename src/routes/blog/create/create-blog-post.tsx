import { mutationRegistry } from "~/utils/mutations";
import { MutationResult } from "~/server/actions/utils";
import { mutateCreateBlogPost } from "~/utils/api";
import { BlogPostInput } from "~/generated/graphql";
import { makeCacheKey } from "~/utils/page-data";
import CreateBlogForm from "~/_components/blog/create/";
import { ServerRedirectError } from "~/utils/server-redirect";

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
  const data = result.data?.blogMutations.createBlogPost as MutationResult;

  switch (data?.__typename) {
    case "QuerySuccess":
      const keyToInvalidate = makeCacheKey("blog", {});
      throw new ServerRedirectError(`/blog/${data.id}`, keyToInvalidate, data);
  }

  return {
    __typename: "StandardError",
    message: result.error || "Failed to create blog post",
  } as const;
}

/**
 * Register the mutation handler for blog post creation.
 */
export function registerBlogCreateMutation(): void {
  mutationRegistry.registerMutationHandler("blog/create", handleCreateBlogPost);
}

export default CreateBlogPostPage;
