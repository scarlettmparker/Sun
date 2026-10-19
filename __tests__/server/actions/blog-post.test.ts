// @vitest-environment jsdom

/**
 * Tests for blog post server actions.
 */

import type { CreateBlogPostResponse } from "~/generated/graphql";
import { createBlogPost } from "~/server/actions/blog-post";

const { mockExecuteMutation } = vi.hoisted(() => ({
  mockExecuteMutation: vi.fn(),
}));

vi.mock("@sun/ssr", () => ({
  executeMutation: (...args: unknown[]) => mockExecuteMutation(...args),
}));

const assignMock = vi.fn();

describe("createBlogPost", () => {
  beforeEach(() => {
    mockExecuteMutation.mockReset();
    assignMock.mockClear();
    Object.defineProperty(window, "location", {
      value: { assign: assignMock },
      writable: true,
    });
  });

  it("should return success response for valid blog post creation", async () => {
    mockExecuteMutation.mockResolvedValue({
      __typename: "CreateBlogPostResponse",
      message: "Blog post created",
      post: { __typename: "BlogPost", id: "1" },
    });

    const result = (await createBlogPost("Test Title", "Test Content")) as CreateBlogPostResponse;

    expect(result.message).toBe("Blog post created");
    expect(mockExecuteMutation).toHaveBeenCalledWith("blog/create", {
      title: "Test Title",
      input: { content: "Test Content" },
    });
    expect(assignMock).toHaveBeenCalledWith("/blog/1");
  });

  it("should trim title and content before delegating", async () => {
    mockExecuteMutation.mockResolvedValue({
      __typename: "CreateBlogPostResponse",
      message: "Blog post created",
      post: { __typename: "BlogPost", id: "1" },
    });

    await createBlogPost("  Title  ", "  Content  ");

    expect(mockExecuteMutation).toHaveBeenCalledWith("blog/create", {
      title: "Title",
      input: { content: "Content" },
    });
  });

  it("should return the mutation result unchanged on failure", async () => {
    mockExecuteMutation.mockResolvedValue({
      __typename: "CreateBlogPostResponse",
      message: "Validation failed",
      post: { __typename: "BlogPost", id: "" },
    });

    const result = await createBlogPost("Title", "Content");

    expect(result.message).toBe("Validation failed");
  });

  it("should throw for invalid input types", async () => {
    await expect(createBlogPost(123 as unknown as string, "Content")).rejects.toThrow(
      "Invalid input",
    );

    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });

  it("should throw for empty title", async () => {
    await expect(createBlogPost("", "Content")).rejects.toThrow("Invalid input");

    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });

  it("should throw for empty content", async () => {
    await expect(createBlogPost("Title", "")).rejects.toThrow("Invalid input");

    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });

  it("should throw for whitespace-only title", async () => {
    await expect(createBlogPost("   ", "Content")).rejects.toThrow("Invalid input");

    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });

  it("should throw for whitespace-only content", async () => {
    await expect(createBlogPost("Title", "   ")).rejects.toThrow("Invalid input");

    expect(mockExecuteMutation).not.toHaveBeenCalled();
  });
});
