/**
 * Tests for server action utilities.
 */

import { executeMutation } from "~/server/actions/utils";

// Mock fetch globally
global.fetch = jest.fn();

const mockFetch = global.fetch as jest.MockedFunction<typeof fetch>;

describe("Server action utilities", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("executeMutation", () => {
    it("should return success response for valid mutation execution", async () => {
      const mockResponse = {
        success: true,
        data: { id: "1", title: "Test Post" },
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result = await executeMutation("blog/create", {
        title: "Test",
        content: "Content",
      });

      expect(result.success).toBe(true);
      expect(result.data).toEqual(mockResponse.data);
      expect(result.error).toBeUndefined();
      expect(mockFetch).toHaveBeenCalledWith("/blog/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: "Test", content: "Content" }),
      });
    });

    it("should return error response for HTTP error", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        statusText: "Bad Request",
      } as unknown as Response);

      const result = await executeMutation("blog/create", { title: "Test" });

      expect(result.success).toBe(false);
      expect(result.error).toBe("HTTP 400: Bad Request");
      expect(result.data).toBeUndefined();
    });

    it("should return error response for network error", async () => {
      mockFetch.mockRejectedValueOnce(new Error("Network error"));

      const result = await executeMutation("blog/create", {});

      expect(result.success).toBe(false);
      expect(result.error).toBe("Network error");
      expect(result.data).toBeUndefined();
    });

    it("should handle mutation response with error", async () => {
      const mockResponse = {
        success: false,
        error: "Validation failed",
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result = await executeMutation("blog/create", { title: "" });

      expect(result.success).toBe(false);
      expect(result.error).toBe("Validation failed");
      expect(result.data).toBeUndefined();
    });
  });
});
