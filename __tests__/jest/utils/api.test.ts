/**
 * Tests for API utility functions.
 */

import { fetchGraphQLData, fetchList, ApiResponse } from "~/utils/api";

// Mock fetch globally
global.fetch = jest.fn();

const mockFetch = global.fetch as jest.MockedFunction<typeof fetch>;

describe("API utilities", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("fetchGraphQLData", () => {
    it("should return success response for valid GraphQL response", async () => {
      const mockResponse = {
        data: { test: "data" },
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result: ApiResponse<unknown> = await fetchGraphQLData("list");

      expect(result.success).toBe(true);
      expect(result.data).toEqual(mockResponse.data);
      expect(result.error).toBeUndefined();
    });

    it("should return error response for HTTP error", async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: "Internal Server Error",
      } as unknown as Response);

      const result: ApiResponse<unknown> = await fetchGraphQLData("list");

      expect(result.success).toBe(false);
      expect(result.error).toBe("HTTP 500: Internal Server Error");
      expect(result.statusCode).toBe(500);
    });

    it("should return error response for GraphQL errors", async () => {
      const mockResponse = {
        errors: [{ message: "Field not found" }],
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result: ApiResponse<unknown> = await fetchGraphQLData("list");

      expect(result.success).toBe(false);
      expect(result.error).toBe("Field not found");
      expect(result.statusCode).toBe(400);
    });

    it("should return error response for network errors", async () => {
      mockFetch.mockRejectedValueOnce(new Error("Network error"));

      const result: ApiResponse<unknown> = await fetchGraphQLData("list");

      expect(result.success).toBe(false);
      expect(result.error).toBe("Network error");
      expect(result.statusCode).toBe(500);
    });

    it("should return error for unknown operation", async () => {
      const result: ApiResponse<unknown> = await fetchGraphQLData(
        "unknown" as string
      );

      expect(result.success).toBe(false);
      expect(result.error).toBe("Unknown operation");
      expect(result.statusCode).toBe(400);
    });
  });

  describe("fetchList", () => {
    it("should call fetchGraphQLData with list operation", async () => {
      const mockResponse = {
        data: { stemPlayerQueries: { list: [] } },
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: jest.fn().mockResolvedValue(mockResponse),
      } as unknown as Response);

      const result = await fetchList();

      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/graphql",
        expect.objectContaining({
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: expect.stringContaining("query"),
        })
      );
      expect(result?.success).toBe(true);
    });
  });
});
