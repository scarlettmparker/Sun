/**
 * Tests for page data utilities.
 */

import { fetchPageData, pageDataRegistry } from "~/utils/page-data";

// Mock console.error to avoid noise in tests
const mockConsoleError = jest.spyOn(console, "error").mockImplementation();

afterAll(() => {
  mockConsoleError.mockRestore();
});

describe("page-data utilities", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Clear all registered loaders before each test
    const registeredPages = pageDataRegistry.getRegisteredPageNames();
    registeredPages.forEach((page) =>
      pageDataRegistry.unregisterPageDataLoader(page)
    );
  });

  describe("pageDataRegistry", () => {
    describe("registerPageDataLoader", () => {
      it("should register a data loader for a page", () => {
        const mockLoader = jest.fn().mockResolvedValue({ test: "data" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);

        expect(pageDataRegistry.hasPageDataLoader("test-page")).toBe(true);
      });
    });

    describe("unregisterPageDataLoader", () => {
      it("should unregister a data loader for a page", () => {
        const mockLoader = jest.fn().mockResolvedValue({ test: "data" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);
        expect(pageDataRegistry.hasPageDataLoader("test-page")).toBe(true);

        pageDataRegistry.unregisterPageDataLoader("test-page");
        expect(pageDataRegistry.hasPageDataLoader("test-page")).toBe(false);
      });
    });

    describe("hasPageDataLoader", () => {
      it("should return true if loader is registered", () => {
        const mockLoader = jest.fn().mockResolvedValue({ test: "data" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);

        expect(pageDataRegistry.hasPageDataLoader("test-page")).toBe(true);
      });

      it("should return false if loader is not registered", () => {
        expect(pageDataRegistry.hasPageDataLoader("non-existent-page")).toBe(
          false
        );
      });
    });

    describe("getRegisteredPageNames", () => {
      it("should return array of registered page names", () => {
        const mockLoader1 = jest.fn().mockResolvedValue({ test: "data1" });
        const mockLoader2 = jest.fn().mockResolvedValue({ test: "data2" });

        pageDataRegistry.registerPageDataLoader("page1", mockLoader1);
        pageDataRegistry.registerPageDataLoader("page2", mockLoader2);

        const registeredPages = pageDataRegistry.getRegisteredPageNames();

        expect(registeredPages).toContain("page1");
        expect(registeredPages).toContain("page2");
        expect(registeredPages).toHaveLength(2);
      });

      it("should return empty array when no loaders are registered", () => {
        const registeredPages = pageDataRegistry.getRegisteredPageNames();

        expect(registeredPages).toEqual([]);
      });
    });
  });

  describe("fetchPageData", () => {
    it("should return data from registered loader", async () => {
      const mockData = { test: "data" };
      const mockLoader = jest.fn().mockResolvedValue(mockData);

      pageDataRegistry.registerPageDataLoader("test-page", mockLoader);

      const result = await fetchPageData("test-page");

      expect(result).toEqual(mockData);
      expect(mockLoader).toHaveBeenCalledTimes(1);
    });

    it("should return null if no loader is registered", async () => {
      const result = await fetchPageData("non-existent-page");

      expect(result).toBeNull();
    });

    it("should return null and log error if loader throws", async () => {
      const mockError = new Error("Loader failed");
      const mockLoader = jest.fn().mockRejectedValue(mockError);

      pageDataRegistry.registerPageDataLoader("test-page", mockLoader);

      const result = await fetchPageData("test-page");

      expect(result).toBeNull();
      expect(mockConsoleError).toHaveBeenCalledWith(
        "Failed to fetch data for page test-page:",
        mockError
      );
    });

    it("should handle loader returning null", async () => {
      const mockLoader = jest.fn().mockResolvedValue(null);

      pageDataRegistry.registerPageDataLoader("test-page", mockLoader);

      const result = await fetchPageData("test-page");

      expect(result).toBeNull();
      expect(mockLoader).toHaveBeenCalledTimes(1);
    });
  });
});
