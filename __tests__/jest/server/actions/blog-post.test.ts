/**
 * Tests for blog post server actions.
 */

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
        success: true,
        data: { id: "1", title: "Test Post", content: "Test content" },
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result = await createBlogPost("Test Title", "Test Content");

      expect(result.success).toBe(true);
      expect(result.data).toEqual(mockResponse.data);
      expect(result.error).toBeUndefined();
      expect(mockFetch).toHaveBeenCalledWith("/blog/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: "Test Title", content: "Test Content" }),
      });
    });

    it("should return error response for HTTP error", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        statusText: "Bad Request",
      } as unknown as Response);

      const result = await createBlogPost("Test Title", "Test Content");

      expect(result.success).toBe(false);
      expect(result.error).toBe("HTTP 400: Bad Request");
      expect(result.data).toBeUndefined();
    });

    it("should return error response for network error", async () => {
      mockFetch.mockRejectedValueOnce(new Error("Network error"));

      const result = await createBlogPost("Test Title", "Test Content");

      expect(result.success).toBe(false);
      expect(result.error).toBe("Network error");
      expect(result.data).toBeUndefined();
    });

    it("should return error for invalid input types", async () => {
      const result = await createBlogPost(
        123 as unknown as string,
        "Test Content"
      );

      expect(result.success).toBe(false);
      expect(result.error).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(result.data).toBeUndefined();
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it("should return error for empty title", async () => {
      const result = await createBlogPost("", "Test Content");

      expect(result.success).toBe(false);
      expect(result.error).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(result.data).toBeUndefined();
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it("should return error for empty content", async () => {
      const result = await createBlogPost("Test Title", "");

      expect(result.success).toBe(false);
      expect(result.error).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(result.data).toBeUndefined();
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it("should return error for whitespace-only title", async () => {
      const result = await createBlogPost("   ", "Test Content");

      expect(result.success).toBe(false);
      expect(result.error).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(result.data).toBeUndefined();
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it("should return error for whitespace-only content", async () => {
      const result = await createBlogPost("Test Title", "   ");

      expect(result.success).toBe(false);
      expect(result.error).toBe(
        "Invalid input: title and content must be non-empty strings"
      );
      expect(result.data).toBeUndefined();
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });
});
