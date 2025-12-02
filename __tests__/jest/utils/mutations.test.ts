/**
 * Tests for mutation utilities.
 */

import {
  restoreConsoleError,
  suppressConsoleErrorsFromTests,
} from "testing/jest/mock";
import {
  mutationRegistry,
  executeMutation,
  clearMutationHandlers,
} from "~/utils/mutations";

// We're forcing errors here and we don't want to bloat our test output
beforeAll(() => {
  suppressConsoleErrorsFromTests();
});

afterAll(() => {
  restoreConsoleError();
});

describe("Mutation utilities", () => {
  beforeEach(() => {
    clearMutationHandlers();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe("executeMutation", () => {
    it("should execute a registered mutation handler successfully", async () => {
      const mockHandler = jest.fn().mockResolvedValue({
        success: true,
        data: { id: "1" },
      });

      mutationRegistry.registerMutationHandler("test/path", mockHandler);

      const result = await executeMutation("test/path", { key: "value" });

      expect(mockHandler).toHaveBeenCalledWith({ key: "value" });
      expect(result.success).toBe(true);
      expect(result.data).toEqual({ id: "1" });
    });

    it("should return error for unknown mutation path", async () => {
      const result = await executeMutation("unknown/path", {});

      expect(result.success).toBe(false);
      expect(result.error).toBe("Unknown mutation path");
    });

    it("should handle errors thrown by mutation handler", async () => {
      const mockHandler = jest
        .fn()
        .mockRejectedValue(new Error("Handler error"));

      mutationRegistry.registerMutationHandler("test/path", mockHandler);

      const result = await executeMutation("test/path", {});

      expect(result.success).toBe(false);
      expect(result.error).toBe("Internal server error");
    });

    it("should return the result from mutation handler", async () => {
      const mockHandler = jest.fn().mockResolvedValue({
        success: false,
        error: "Validation failed",
      });

      mutationRegistry.registerMutationHandler("test/path", mockHandler);

      const result = await executeMutation("test/path", {});

      expect(result.success).toBe(false);
      expect(result.error).toBe("Validation failed");
    });
  });

  describe("mutationRegistry", () => {
    it("should register and execute mutation handlers", async () => {
      const mockHandler = jest.fn().mockResolvedValue({
        success: true,
      });

      mutationRegistry.registerMutationHandler("blog/create", mockHandler);

      const result = await mutationRegistry.executeMutation("blog/create", {
        title: "Test",
      });

      expect(mockHandler).toHaveBeenCalledWith({ title: "Test" });
      expect(result.success).toBe(true);
    });
  });
});
