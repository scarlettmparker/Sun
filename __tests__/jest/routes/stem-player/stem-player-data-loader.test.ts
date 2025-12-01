/**
 * Tests for stem-player data loader.
 */

import { ApiResponse, fetchListSongs } from "~/utils/api";
import { pageDataRegistry } from "~/utils/page-data";
import {
  getStemPlayerData,
  registerStemPlayerDataLoader,
} from "~/routes/stem-player/stem-player";
import { Song } from "~/generated/graphql";

// Mock the API function
jest.mock("~/utils/api", () => ({
  fetchListSongs: jest.fn(),
}));

const mockFetchListSongs = fetchListSongs as jest.MockedFunction<
  typeof fetchListSongs
>;

describe("Stem Player Data Loader", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Clear all registered loaders before each test
    const registeredPages = pageDataRegistry.getRegisteredPageNames();
    registeredPages.forEach((page) =>
      pageDataRegistry.unregisterPageDataLoader(page)
    );
  });

  describe("getStemPlayerData", () => {
    it("should return songs data when fetchListSongs succeeds", async () => {
      const mockSongs = [
        { id: "1", name: "Song 1", stems: [] },
        { id: "2", name: "Song 2", stems: [] },
      ];
      const mockResponse = {
        success: true,
        data: {
          stemPlayerQueries: {
            list: mockSongs,
          },
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toEqual({
        songs: mockSongs,
      });
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should return null when fetchListSongs fails", async () => {
      const mockResponse = {
        success: false,
        error: "API Error",
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should return null when fetchListSongs throws an error", async () => {
      const mockConsoleError = jest
        .spyOn(console, "error")
        .mockImplementation();

      const mockError = new Error("Network error");

      mockFetchListSongs.mockRejectedValue(mockError);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
      mockConsoleError.mockRestore();
    });

    it("should return null when response data is missing", async () => {
      const mockResponse = {
        success: true,
        data: null,
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should return null when stemPlayerQueries is missing", async () => {
      // Due to intentionally throwing
      const mockConsoleError = jest
        .spyOn(console, "error")
        .mockImplementation();

      const mockResponse = {
        success: true,
        data: {},
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);

      mockConsoleError.mockRestore();
    });

    it("should return null when list is missing from stemPlayerQueries", async () => {
      const mockResponse = {
        success: true,
        data: {
          stemPlayerQueries: {},
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should handle empty songs array", async () => {
      const mockSongs: Song[] = [];
      const mockResponse = {
        success: true,
        data: {
          stemPlayerQueries: {
            list: mockSongs,
          },
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toEqual({
        songs: [],
      });
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should handle malformed response data", async () => {
      // Due to intentionally throwing
      const mockConsoleError = jest
        .spyOn(console, "error")
        .mockImplementation();

      const mockResponse = {
        success: true,
        data: "not-an-object",
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);

      mockConsoleError.mockRestore();
    });

    it("should handle response with wrong structure", async () => {
      // Due to intentionally throwing
      const mockConsoleError = jest
        .spyOn(console, "error")
        .mockImplementation();

      const mockResponse = {
        success: true,
        data: {
          wrongKey: {
            list: [{ id: "1", name: "Song 1" }],
          },
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);

      mockConsoleError.mockRestore();
    });
  });

  describe("Data loader registration", () => {
    it("should register the data loader with pageDataRegistry", () => {
      registerStemPlayerDataLoader();

      expect(pageDataRegistry.hasPageDataLoader("stem-player")).toBe(true);
    });

    it("should handle multiple registrations gracefully", () => {
      registerStemPlayerDataLoader();
      registerStemPlayerDataLoader();

      expect(pageDataRegistry.hasPageDataLoader("stem-player")).toBe(true);
    });
  });

  describe("Integration with page data fetching", () => {
    it("should be callable through fetchPageData after registration", async () => {
      const { fetchPageData } = await import("~/utils/page-data");

      registerStemPlayerDataLoader();

      const mockSongs = [{ id: "1", name: "Song 1", stems: [] }];
      const mockResponse = {
        success: true,
        data: {
          stemPlayerQueries: {
            list: mockSongs,
          },
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await fetchPageData("stem-player");

      expect(result).toEqual({
        songs: mockSongs,
      });
    });

    it("should handle params passed to fetchPageData", async () => {
      const { fetchPageData } = await import("~/utils/page-data");

      registerStemPlayerDataLoader();

      const mockSongs = [{ id: "1", name: "Song 1", stems: [] }];
      const mockResponse = {
        success: true,
        data: {
          stemPlayerQueries: {
            list: mockSongs,
          },
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const params = { filter: "popular" };
      const result = await fetchPageData("stem-player", params);

      expect(result).toEqual({
        songs: mockSongs,
      });
    });
  });

  describe("Error handling edge cases", () => {
    it("should handle fetchListSongs returning undefined", async () => {
      mockFetchListSongs.mockResolvedValue(
        undefined as unknown as
          | ApiResponse<unknown>
          | Promise<ApiResponse<unknown>>
      );

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should handle fetchListSongs returning null", async () => {
      mockFetchListSongs.mockResolvedValue(
        null as unknown as ApiResponse<unknown> | Promise<ApiResponse<unknown>>
      );

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should handle response with success undefined", async () => {
      const mockResponse = {
        success: false,
        data: {
          stemPlayerQueries: {
            list: [{ id: "1", name: "Song 1" }],
          },
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should handle deeply nested null values", async () => {
      const mockResponse = {
        success: true,
        data: {
          stemPlayerQueries: {
            list: null,
          },
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should handle songs with missing required fields", async () => {
      const mockSongs = [{ id: "1" }, { name: "Song 2" }];
      const mockResponse = {
        success: true,
        data: {
          stemPlayerQueries: {
            list: mockSongs,
          },
        },
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toEqual({
        songs: mockSongs,
      });
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });
  });
});
