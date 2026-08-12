/**
 * Tests for blog post server actions.
 */

import type { StandardError, QuerySuccess } from "~/generated/graphql";
import { createBlogPost } from "~/server/actions/blog-post";

const { mockExecuteMutation } = vi.hoisted(() => ({
  mockExecuteMutation: vi.fn(),
}));

vi.mock("@sun/ssr", () => ({
  executeMutation: (...args: unknown[]) => mockExecuteMutation(...args),
}));

describe("createBlogPost", () => {
  beforeEach(() => {
    mockExecuteMutation.mockReset();
  });

  it("should return success response for valid blog post creation", async () => {
    mockExecuteMutation.mockResolvedValue({
      __typename: "QuerySuccess",
      message: "Blog post created",
      id: "1",
    });

    const result = (await createBlogPost(
      "Test Title",
      "Test Content",
    )) as QuerySuccess;

    expect(result.__typename).toBe("QuerySuccess");
    expect(mockExecuteMutation).toHaveBeenCalledWith("blog/create", {
      title: "Test Title",
      input: { content: "Test Content" },
    });
  });

  it("should trim title and content before delegating", async () => {
    mockExecuteMutation.mockResolvedValue({
      __typename: "QuerySuccess",
      message: "Blog post created",
      id: "1",
    });

    await createBlogPost("  Title  ", "  Content  ");

    expect(mockExecuteMutation).toHaveBeenCalledWith("blog/create", {
      title: "Title",
      input: { content: "Content" },
    });
  });

  it("should return the mutation result unchanged on failure", async () => {
    mockExecuteMutation.mockResolvedValue({
      __typename: "StandardError",
      message: "Validation failed",
    });

    const result = await createBlogPost("Title", "Content");

    expect((result as StandardError).__typename).toBe("StandardError");
    expect((result as StandardError).message).toBe("Validation failed");
  });

  it("should return error for invalid input types", async () => {
    const result = await createBlogPost(123 as unknown as string, "Content");

    expect((result as StandardError).__typename).toBe("StandardError");
    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });

  it("should return error for empty title", async () => {
    const result = await createBlogPost("", "Content");

    expect((result as StandardError).__typename).toBe("StandardError");
    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });

  it("should return error for empty content", async () => {
    const result = await createBlogPost("Title", "");

    expect((result as StandardError).__typename).toBe("StandardError");
    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });

  it("should return error for whitespace-only title", async () => {
    const result = await createBlogPost("   ", "Content");

    expect((result as StandardError).__typename).toBe("StandardError");
    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });

  it("should return error for whitespace-only content", async () => {
    const result = await createBlogPost("Title", "   ");

    expect((result as StandardError).__typename).toBe("StandardError");
    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });
});
