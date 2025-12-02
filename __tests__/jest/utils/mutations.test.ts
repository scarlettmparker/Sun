/**
 * Tests for mutation utilities.
 */

import { QuerySuccess } from "~/generated/graphql";
import {
  mutationRegistry,
  executeMutation,
  clearMutationHandlers,
} from "~/utils/mutations";

// We're forcing errors here and we don't want to bloat our test output
const mockConsoleError = jest.spyOn(console, "error").mockImplementation();

afterAll(() => {
  mockConsoleError.mockRestore();
});

describe("Mutation utilities", () => {
  beforeEach(() => {
    clearMutationHandlers();
    jest.clearAllMocks();
  });

  describe("executeMutation", () => {
    it("should execute a registered mutation handler successfully", async () => {
      const mockHandler = jest.fn().mockResolvedValue({
        __typename: "QuerySuccess",
        message: "Success",
        id: "1",
      });

      mutationRegistry.registerMutationHandler("test/path", mockHandler);

      const result = (await executeMutation("test/path", {
        key: "value",
      })) as QuerySuccess;

      expect(mockHandler).toHaveBeenCalledWith({ key: "value" });
      expect(result.__typename).toBe("QuerySuccess");
      expect(result.message).toBe("Success");
      expect(result.id).toBe("1");
    });

    it("should return error for unknown mutation path", async () => {
      const result = await executeMutation("unknown/path", {});

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe("Unknown mutation path");
    });

    it("should handle errors thrown by mutation handler", async () => {
      const mockHandler = jest
        .fn()
        .mockRejectedValue(new Error("Handler error"));

      mutationRegistry.registerMutationHandler("test/path", mockHandler);

      const result = await executeMutation("test/path", {});

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe("Internal server error");
    });

    it("should return the result from mutation handler", async () => {
      const mockHandler = jest.fn().mockResolvedValue({
        __typename: "StandardError",
        message: "Validation failed",
      });

      mutationRegistry.registerMutationHandler("test/path", mockHandler);

      const result = await executeMutation("test/path", {});

      expect(result.__typename).toBe("StandardError");
      expect(result.message).toBe("Validation failed");
    });
  });

  describe("mutationRegistry", () => {
    it("should register and execute mutation handlers", async () => {
      const mockHandler = jest.fn().mockResolvedValue({
        __typename: "QuerySuccess",
        message: "Success",
        id: "1",
      });

      mutationRegistry.registerMutationHandler("blog/create", mockHandler);

      const result = await mutationRegistry.executeMutation("blog/create", {
        title: "Test",
      });

      expect(mockHandler).toHaveBeenCalledWith({ title: "Test" });
      expect(result.__typename).toBe("QuerySuccess");
    });
  });
});
