/**
 * Tests for stem-player data loader.
 */

import { fetchListSongs } from "~/utils/api";
import { pageDataRegistry } from "~/utils/page-data";
import {
  getStemPlayerData,
  registerStemPlayerDataLoader,
} from "~/routes/stem-player/stem-player";
import {
  restoreConsoleError,
  suppressConsoleErrorsFromTests,
} from "testing/jest/mock";

// Mock the API function
jest.mock("~/utils/api", () => ({
  fetchListSongs: jest.fn(),
}));

const mockFetchListSongs = fetchListSongs as jest.MockedFunction<
  typeof fetchListSongs
>;

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

  it("should register the stem-player data loader", () => {
    // Call the registration function
    registerStemPlayerDataLoader();

    expect(pageDataRegistry.hasPageDataLoader("stem-player")).toBe(true);
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
            listSongs: mockSongs,
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
      const mockError = new Error("Network error");

      mockFetchListSongs.mockRejectedValue(mockError);

      const result = await getStemPlayerData();

      expect(result).toBeNull();
      expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
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
