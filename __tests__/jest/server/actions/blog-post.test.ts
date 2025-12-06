/**
 * Tests for blog post server actions.
 */

jest.unmock("~/server/actions/blog-post");

import { QuerySuccess } from "~/generated/graphql";
import { createBlogPost } from "~/server/actions/blog-post";

// Mock fetch globally
global.fetch = jest.fn();

const mockFetch = global.fetch as jest.MockedFunction<typeof fetch>;

describe("Blog post server actions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("createBlogPost", () => {
    it("should return success response for valid blog post creation", async () => {
      const mockResponse = {
        __typename: "QuerySuccess",
        message: "Blog post created",
        id: "1",
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result = (await createBlogPost(
        "Test Title",
        "Test Content"
      )) as QuerySuccess;

      expect(result.__typename).toBe("QuerySuccess");
      expect(result.message).toBe("Blog post created");
      expect(result.id).toBe("1");
      expect(mockFetch).toHaveBeenCalledWith("/blog/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: "Test Title",
          input: { content: "Test Content" },
        }),
      });
    });

    it("should return error response for HTTP error", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        statusText: "Bad Request",
      } as unknown as Response);

      const result = await createBlogPost("Test Title", "Test Content");

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe("HTTP 400: Bad Request");
    });

    it("should return error response for network error", async () => {
      mockFetch.mockRejectedValueOnce(new Error("Network error"));

      const result = await createBlogPost("Test Title", "Test Content");

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe("Network error");
    });

    it("should return error for invalid input types", async () => {
      const result = await createBlogPost(
        123 as unknown as string,
        "Test Content"
      );

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it("should return error for empty title", async () => {
      const result = await createBlogPost("", "Test Content");

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it("should return error for empty content", async () => {
      const result = await createBlogPost("Test Title", "");

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it("should return error for whitespace-only title", async () => {
      const result = await createBlogPost("   ", "Test Content");

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it("should return error for whitespace-only content", async () => {
      const result = await createBlogPost("Test Title", "   ");

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });
});
