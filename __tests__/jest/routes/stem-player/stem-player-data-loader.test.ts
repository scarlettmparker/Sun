/**
 * Tests for stem-player data loader.
 */

import { fetchList } from "~/utils/api";
import { pageDataRegistry } from "~/utils/page-data";
import { getStemPlayerData } from "~/routes/stem-player/stem-player";
import {
  restoreConsoleError,
  suppressConsoleErrorsFromTests,
} from "testing/jest/mock";

// Mock the API function
jest.mock("~/utils/api", () => ({
  fetchList: jest.fn(),
}));

const mockFetchListSongs = fetchList as jest.MockedFunction<typeof fetchList>;

beforeAll(() => {
  // Due to network errors from non existent gql server in mocked env
  suppressConsoleErrorsFromTests();
});

afterAll(() => {
  restoreConsoleError();
});

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
    it("should return songs data when fetchList succeeds", async () => {
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

    it("should return null when fetchList fails", async () => {
      const mockResponse = {
        success: false,
        error: "API Error",
      };

      mockFetchListSongs.mockResolvedValue(mockResponse);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
    });

    it("should return null when fetchList throws an error", async () => {
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
  });
});
