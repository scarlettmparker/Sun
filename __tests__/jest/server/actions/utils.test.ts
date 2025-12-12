/**
 * Tests for server action utilities.
 */

import { QuerySuccess, StandardError } from "~/generated/graphql";
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
        __typename: "QuerySuccess",
        message: "Success",
        id: "1",
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result = (await executeMutation("blog/create", {
        title: "Test",
        content: "Content",
      })) as QuerySuccess;

      expect(result.__typename).toBe("QuerySuccess");
      expect(result.message).toBe("Success");
      expect(result.id).toBe("1");
      expect(mockFetch).toHaveBeenCalledWith("/blog/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        redirect: "manual",
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

      expect((result as StandardError).__typename).toBe("StandardError");
      expect((result as StandardError).message).toBe("HTTP 400: Bad Request");
    });

    it("should return error response for network error", async () => {
      mockFetch.mockRejectedValueOnce(new Error("Network error"));

      const result = await executeMutation("blog/create", {});

      expect((result as StandardError).__typename).toBe("StandardError");
      expect((result as StandardError).message).toBe("Network error");
    });

    it("should handle mutation response with error", async () => {
      const mockResponse = {
        __typename: "StandardError",
        message: "Validation failed",
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result = await executeMutation("blog/create", { title: "" });

      expect((result as StandardError).__typename).toBe("StandardError");
      expect((result as StandardError).message).toBe("Validation failed");
    });
  });
});
