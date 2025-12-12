/**
 * Tests for page data utilities.
 */

declare const global: typeof globalThis & {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  window?: any;
};

import {
  fetchPageData,
  pageDataRegistry,
  invalidateCache,
  makeCacheKey,
  usePageData,
  suspenseCache,
} from "~/utils/page-data";

const mockConsoleError = jest.spyOn(console, "error").mockImplementation();

afterAll(() => {
  mockConsoleError.mockRestore();
});

describe("page-data utilities", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    const registeredPages = pageDataRegistry.getRegisteredPageNames();
    registeredPages.forEach((page) =>
      pageDataRegistry.unregisterPageDataLoader(page)
    );
    suspenseCache.clear();
  });

  describe("pageDataRegistry", () => {
    describe("registerPageDataLoader", () => {
      it("should register a data loader for a page", () => {
        const mockLoader = jest.fn().mockResolvedValue({ test: "data" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);

        expect(pageDataRegistry.hasPageDataLoader("test-page")).toBe(true);
      });

      it("should allow registering multiple loaders for the same page", () => {
        const mockLoader1 = jest.fn().mockResolvedValue({ test: "data1" });
        const mockLoader2 = jest.fn().mockResolvedValue({ test: "data2" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader1);
        pageDataRegistry.registerPageDataLoader("test-page", mockLoader2);

        expect(pageDataRegistry.hasPageDataLoader("test-page")).toBe(true);
      });

      it("should not register the same loader function twice", () => {
        const mockLoader = jest.fn().mockResolvedValue({ test: "data" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);
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

      it("should clear cache when unregistering", () => {
        const mockLoader = jest.fn().mockResolvedValue({ test: "data" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);
        const cacheKey = "test-page";
        const cache = pageDataRegistry.pageDataCache;
        cache[cacheKey] = { data: { test: "cached" }, timestamp: Date.now() };

        pageDataRegistry.unregisterPageDataLoader("test-page");

        expect(cache[cacheKey]).toBeUndefined();
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

      it("should return false after unregistering", () => {
        const mockLoader = jest.fn().mockResolvedValue({ test: "data" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);
        expect(pageDataRegistry.hasPageDataLoader("test-page")).toBe(true);

        pageDataRegistry.unregisterPageDataLoader("test-page");
        expect(pageDataRegistry.hasPageDataLoader("test-page")).toBe(false);
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

      it("should not duplicate page names when multiple loaders are registered", () => {
        const mockLoader1 = jest.fn().mockResolvedValue({ test: "data1" });
        const mockLoader2 = jest.fn().mockResolvedValue({ test: "data2" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader1);
        pageDataRegistry.registerPageDataLoader("test-page", mockLoader2);

        const registeredPages = pageDataRegistry.getRegisteredPageNames();

        expect(registeredPages).toEqual(["test-page"]);
      });
    });
  });

  describe("fetchPageData", () => {
    it("should return data from registered loader (exact match)", async () => {
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
        "Failed to fetch data for pattern test-page:",
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

    it("should merge data from multiple loaders (same pattern)", async () => {
      const mockLoader1 = jest.fn().mockResolvedValue({ data1: "value1" });
      const mockLoader2 = jest.fn().mockResolvedValue({ data2: "value2" });

      pageDataRegistry.registerPageDataLoader("test-page", mockLoader1);
      pageDataRegistry.registerPageDataLoader("test-page", mockLoader2);

      const result = await fetchPageData("test-page");

      expect(result).toEqual({ data1: "value1", data2: "value2" });
      expect(mockLoader1).toHaveBeenCalledTimes(1);
      expect(mockLoader2).toHaveBeenCalledTimes(1);
    });

    it("should pass params to loaders (exact match with params)", async () => {
      const mockLoader = jest.fn().mockResolvedValue({ test: "data" });
      const path = "blog/:id";
      const url = "blog/456";

      pageDataRegistry.registerPageDataLoader(path, mockLoader);

      await fetchPageData(url);

      expect(mockLoader).toHaveBeenCalledWith({ id: "456" });
    });

    it("should run only the layout loader for the base path", async () => {
      const layoutLoader = jest.fn().mockResolvedValue({ layout: "header" });
      const specificLoader = jest
        .fn()
        .mockResolvedValue({ detail: "song-info" });

      pageDataRegistry.registerPageDataLoader("stem-player", layoutLoader);
      pageDataRegistry.registerPageDataLoader(
        "stem-player/:id",
        specificLoader
      );

      const urlPath = "/stem-player";
      const result = await fetchPageData(urlPath);

      expect(layoutLoader).toHaveBeenCalledTimes(1);
      expect(specificLoader).not.toHaveBeenCalled();

      expect(result).toEqual({ layout: "header" });
    });

    it("should use cache for repeated calls within expiration time in production", async () => {
      const originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = "production";

      try {
        const mockData = { test: "cached-data" };
        const mockLoader = jest.fn().mockResolvedValue(mockData);

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);

        const result1 = await fetchPageData("test-page");
        expect(result1).toEqual(mockData);
        expect(mockLoader).toHaveBeenCalledTimes(1);

        const result2 = await fetchPageData("test-page");
        expect(result2).toEqual(mockData);
        expect(mockLoader).toHaveBeenCalledTimes(1);
      } finally {
        process.env.NODE_ENV = originalEnv;
      }
    });

    it("should not use cache in development", async () => {
      const originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = "development";

      try {
        const mockData = { test: "fresh-data" };
        const mockLoader = jest.fn().mockResolvedValue(mockData);

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader);

        const result1 = await fetchPageData("test-page");
        expect(result1).toEqual(mockData);
        expect(mockLoader).toHaveBeenCalledTimes(1);

        const result2 = await fetchPageData("test-page");
        expect(result2).toEqual(mockData);
        expect(mockLoader).toHaveBeenCalledTimes(2);
      } finally {
        process.env.NODE_ENV = originalEnv;
      }
    });

    it("should invalidate cache when loader is re-registered in production", async () => {
      const originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = "production";

      try {
        const mockLoader1 = jest.fn().mockResolvedValue({ data: "old" });
        const mockLoader2 = jest.fn().mockResolvedValue({ data: "new" });

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader1);
        await fetchPageData("test-page");

        pageDataRegistry.registerPageDataLoader("test-page", mockLoader2);
        const result = await fetchPageData("test-page");

        expect(result).toEqual({ data: "new" });
        expect(mockLoader1).toHaveBeenCalledTimes(2);
        expect(mockLoader2).toHaveBeenCalledTimes(1);
      } finally {
        process.env.NODE_ENV = originalEnv;
      }
    });

    it("should return null and log error if one loader fails in a multi-loader scenario (same pattern)", async () => {
      const mockLoader1 = jest.fn().mockResolvedValue({ data1: "value1" });
      const mockLoader2 = jest.fn().mockRejectedValue(new Error("Failed"));

      pageDataRegistry.registerPageDataLoader("test-page", mockLoader1);
      pageDataRegistry.registerPageDataLoader("test-page", mockLoader2);

      const result = await fetchPageData("test-page");

      expect(result).toBeNull();
      expect(mockConsoleError).toHaveBeenCalledWith(
        "Failed to fetch data for pattern test-page:",
        expect.any(Error)
      );
    });

    it("should return null and log error if one loader fails in a nested route scenario", async () => {
      const layoutLoader = jest.fn().mockResolvedValue({ layout: "header" });
      const specificLoader = jest
        .fn()
        .mockRejectedValue(new Error("Failed Detail Load"));

      pageDataRegistry.registerPageDataLoader("stem-player", layoutLoader);
      pageDataRegistry.registerPageDataLoader(
        "stem-player/:id",
        specificLoader
      );

      const urlPath = "/stem-player/id";
      const result = await fetchPageData(urlPath);

      expect(result).toBeNull();
      expect(mockConsoleError).toHaveBeenCalledWith(
        "Failed to fetch data for pattern stem-player/:id:",
        expect.any(Error)
      );
    });

    it("should handle non-object results from loaders", async () => {
      const mockLoader1 = jest.fn().mockResolvedValue({ data1: "value1" });
      const mockLoader2 = jest.fn().mockResolvedValue("string-result");

      pageDataRegistry.registerPageDataLoader("test-page", mockLoader1);
      pageDataRegistry.registerPageDataLoader("test-page", mockLoader2);

      const result = await fetchPageData("test-page");

      expect(result).toEqual({ data1: "value1" });
    });
  });

  describe("invalidateCache", () => {
    it("should invalidate cache for a specific pattern and params", () => {
      const cache = pageDataRegistry.pageDataCache;
      const pattern = "test-page";
      const params = { id: "123" };
      const cacheKey = `${pattern}:${JSON.stringify(params)}`;

      cache[cacheKey] = { data: { test: "cached" }, timestamp: Date.now() };

      invalidateCache(pattern, params);

      expect(cache[cacheKey]).toBeUndefined();
    });

    it("should invalidate cache when params is undefined", () => {
      const cache = pageDataRegistry.pageDataCache;
      const pattern = "test-page";
      const cacheKey = `${pattern}:${JSON.stringify({})}`;

      cache[cacheKey] = { data: { test: "cached" }, timestamp: Date.now() };

      invalidateCache(pattern);

      expect(cache[cacheKey]).toBeUndefined();
    });

    it("should not affect other cache entries", () => {
      const cache = pageDataRegistry.pageDataCache;
      const pattern1 = "test-page1";
      const pattern2 = "test-page2";
      const params1 = { id: "123" };
      const params2 = { id: "456" };
      const cacheKey1 = `${pattern1}:${JSON.stringify(params1)}`;
      const cacheKey2 = `${pattern2}:${JSON.stringify(params2)}`;

      cache[cacheKey1] = { data: { test: "cached1" }, timestamp: Date.now() };
      cache[cacheKey2] = { data: { test: "cached2" }, timestamp: Date.now() };

      invalidateCache(pattern1, params1);

      expect(cache[cacheKey1]).toBeUndefined();
      expect(cache[cacheKey2]).toEqual({
        data: { test: "cached2" },
        timestamp: expect.any(Number),
      });
    });
  });

  describe("makeCacheKey", () => {
    it("should create a normalized cache key with pattern and params", () => {
      const pattern = "blog";
      const params = { id: "123" };
      const result = makeCacheKey(pattern, params);

      expect(result).toBe('/blog:{"id":"123"}');
    });

    it("should normalize pattern by adding leading slash if missing", () => {
      const pattern = "blog";
      const result = makeCacheKey(pattern);

      expect(result).toBe("/blog:{}");
    });

    it("should handle pattern that already has leading slash", () => {
      const pattern = "/blog";
      const result = makeCacheKey(pattern);

      expect(result).toBe("/blog:{}");
    });

    it("should stringify empty params as empty object", () => {
      const pattern = "blog";
      const result = makeCacheKey(pattern, {});

      expect(result).toBe("/blog:{}");
    });

    it("should handle undefined params", () => {
      const pattern = "blog";
      const result = makeCacheKey(pattern, undefined);

      expect(result).toBe("/blog:{}");
    });
  });

  describe("usePageData", () => {
    it("should return data from loader on server side", async () => {
      const mockLoader = jest.fn().mockResolvedValue({ posts: [{ id: 1 }] });
      pageDataRegistry.registerPageDataLoader("blog", mockLoader);
      expect(() => usePageData("posts", "blog")).toThrow(Promise);
    });

    it("should return resolved data on client side if cached", () => {
      // Simulate client
      const originalWindow = global.window;
      global.window = {};

      try {
        const cacheKey = makeCacheKey("blog");
        suspenseCache.set(cacheKey, {
          status: "resolved",
          result: { posts: [{ id: 1 }] },
        });

        const result = usePageData("posts", "blog");

        expect(result).toEqual({ data: [{ id: 1 }] });
      } finally {
        global.window = originalWindow;
      }
    });

    it("should throw promise on client side if pending", () => {
      const originalWindow = global.window;
      global.window = {};

      try {
        const cacheKey = makeCacheKey("blog");
        const mockPromise = Promise.resolve();
        suspenseCache.set(cacheKey, {
          status: "pending",
          promise: mockPromise,
        });

        expect(() => usePageData("posts", "blog")).toThrow();
      } finally {
        global.window = originalWindow;
      }
    });

    it("should throw error on client side if rejected", () => {
      const originalWindow = global.window;
      global.window = {};

      try {
        const cacheKey = makeCacheKey("blog");
        const mockError = new Error("Failed");
        suspenseCache.set(cacheKey, {
          status: "rejected",
          error: mockError,
        });

        expect(() => usePageData("posts", "blog")).toThrow(mockError);
      } finally {
        global.window = originalWindow;
      }
    });
  });
});
