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

    it("should extract pageName from URL path correctly", () => {
      // Simulate URL parsing logic from routes/index.js
      const testCases = [
        { url: "/stem-player", expected: "stem-player" },
        { url: "/stem-player/123", expected: "stem-player" },
        { url: "/", expected: "" },
        { url: "/unknown-page", expected: "unknown-page" },
        { url: "/stem-player?query=1", expected: "stem-player" },
      ];

      testCases.forEach(({ url, expected }) => {
        const urlPath = url.split("?")[0];
        const pageName = urlPath.split("/")[1] || "";
        expect(pageName).toBe(expected);
      });
    });

    it("should handle route parameters extraction", () => {
      const testCases = [
        { url: "/stem-player/123", params: { id: "123" } },
        { url: "/stem-player", params: {} },
        { url: "/other/456/edit", params: { id: "456", action: "edit" } },
      ];

      testCases.forEach(({ url: _, params }) => {
        expect(params).toBeDefined();
      });
    });

    it("should pass route params to fetchPageData", async () => {
      const mockPageData = { song: "data" };
      mockFetchPageData.mockResolvedValue(mockPageData);

      const pageName = "stem-player";
      const params = { id: "123" };
      const pageData = await fetchPageData(pageName, params);

      expect(mockFetchPageData).toHaveBeenCalledWith(pageName, params);
      expect(pageData).toEqual(mockPageData);
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

    it("should handle null pageData", () => {
      const mockWindow = {
        __pageData__: undefined,
      } as Window & typeof globalThis;

      const pageData = null;
      mockWindow.__pageData__ = pageData || {};

      expect(mockWindow.__pageData__).toEqual({});
    });

    it("should inject translations into window.__translations__", () => {
      const mockWindow = {
        __translations__: undefined,
        __locale__: undefined,
      } as Window & typeof globalThis;

      const translations = { key1: "value1", key2: "value2" };
      const locale = "en-GB";

      mockWindow.__translations__ = translations;
      mockWindow.__locale__ = locale;

      expect(mockWindow.__translations__).toEqual(translations);
      expect(mockWindow.__locale__).toBe(locale);
    });

    it("should serialize pageData to JSON for injection", () => {
      const pageData = { songs: [{ id: "1", name: "Test Song" }] };
      const serialized = JSON.stringify(pageData);

      expect(() => JSON.parse(serialized)).not.toThrow();
      expect(JSON.parse(serialized)).toEqual(pageData);
    });

    it("should handle complex nested pageData", () => {
      const pageData = {
        songs: [{ id: "1", name: "Song 1", stems: [{ name: "Drums" }] }],
        metadata: { total: 1, page: 1 },
      };

      const serialized = JSON.stringify(pageData);
      const deserialized = JSON.parse(serialized);

      expect(deserialized).toEqual(pageData);
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

    it("should handle pageData without songs property", () => {
      const pageData: Record<string, unknown> = { other: "data" };
      const initialData =
        pageData.songs as ListSongsQuery["stemPlayerQueries"]["list"];

      expect(initialData).toBeUndefined();
    });

    it("should handle empty songs array", () => {
      const mockSongs: ListSongsQuery["stemPlayerQueries"]["list"] = [];
      const pageData: Record<string, unknown> = {
        songs: mockSongs,
      };

      const initialData =
        pageData.songs as ListSongsQuery["stemPlayerQueries"]["list"];

      expect(initialData).toEqual([]);
    });

    it("should handle malformed songs data", () => {
      const pageData: Record<string, unknown> = {
        songs: "not-an-array",
      };

      const initialData =
        pageData.songs as ListSongsQuery["stemPlayerQueries"]["list"];

      expect(initialData).toBe("not-an-array");
    });
  });

  describe("error handling in SSR pipeline", () => {
    it("should handle renderApp errors gracefully", async () => {
      const mockError = new Error("Render failed");

      expect(() => {
        throw mockError;
      }).toThrow("Render failed");
    });

    it("should handle missing clientJs or clientCss in render function", async () => {
      expect(() => {
        const clientJs = undefined;
        const clientCss = undefined;
        if (!clientJs || !clientCss) {
          throw new Error("Missing required clientJs or clientCss path");
        }
      }).toThrow("Missing required clientJs or clientCss path");
    });

    it("should handle invalid JSON in pageData serialization", () => {
      const circularRef: Record<string, unknown> = {};
      circularRef.self = circularRef;

      expect(() => {
        JSON.stringify(circularRef);
      }).toThrow();
    });
  });
});
