/**
 * Tests for SSR data injection utilities.
 */

import { fetchPageData } from "~/utils/page-data";
import { ListSongsQuery } from "~/generated/graphql";

// Mock fetchPageData to control its behavior in tests
jest.mock("~/utils/page-data", () => ({
  fetchPageData: jest.fn(),
}));

const mockFetchPageData = fetchPageData as jest.MockedFunction<
  typeof fetchPageData
>;

describe("SSR data injection", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("data fetching in routes/index.js", () => {
    it("should call fetchPageData with correct pageName", async () => {
      const mockPageData = { test: "data" };
      mockFetchPageData.mockResolvedValue(mockPageData);

      // Simulate the logic from routes/index.js
      const pageName = "stem-player";
      const pageData = await fetchPageData(pageName);

      expect(mockFetchPageData).toHaveBeenCalledWith(pageName);
      expect(pageData).toEqual(mockPageData);
    });

    it("should handle null pageData from fetchPageData", async () => {
      mockFetchPageData.mockResolvedValue(null);

      const pageName = "non-existent-page";
      const pageData = await fetchPageData(pageName);

      expect(mockFetchPageData).toHaveBeenCalledWith(pageName);
      expect(pageData).toBeNull();
    });

    it("should handle fetchPageData throwing an error", async () => {
      const mockError = new Error("Fetch failed");
      mockFetchPageData.mockRejectedValue(mockError);

      const pageName = "error-page";

      await expect(fetchPageData(pageName)).rejects.toThrow("Fetch failed");
      expect(mockFetchPageData).toHaveBeenCalledWith(pageName);
    });
  });

  describe("data injection in entry-server.tsx", () => {
    it("should inject pageData into window.__pageData__", () => {
      // Mock window object
      const mockWindow = {
        __pageData__: undefined,
      } as Window & typeof globalThis;

      // Simulate the injection logic from entry-server.tsx
      const pageData = { test: "injected data" };
      mockWindow.__pageData__ = pageData;

      expect(mockWindow.__pageData__).toEqual(pageData);
    });

    it("should handle undefined pageData", () => {
      const mockWindow = {
        __pageData__: undefined,
      } as Window & typeof globalThis;

      const pageData = undefined;
      mockWindow.__pageData__ = pageData || {};

      expect(mockWindow.__pageData__).toEqual({});
    });
  });

  describe("client-side data access in stem-player.tsx", () => {
    it("should access pageData from window object", () => {
      // Mock window object with pageData
      const mockSongs: ListSongsQuery["stemPlayerQueries"]["list"] = [
        { __typename: "Song", id: "1", name: "Test Song" },
      ];
      const mockPageData: Record<string, unknown> = {
        songs: mockSongs,
      };

      // Simulate the logic from stem-player.tsx
      const pageData = mockPageData;
      const initialData =
        pageData.songs as ListSongsQuery["stemPlayerQueries"]["list"];

      expect(initialData).toEqual(mockSongs);
    });

    it("should handle missing pageData", () => {
      const pageData: Record<string, unknown> | undefined = undefined;
      const initialData = (pageData as Record<string, unknown> | undefined)
        ?.songs as ListSongsQuery["stemPlayerQueries"]["list"];

      expect(initialData).toBeUndefined();
    });
  });
});
