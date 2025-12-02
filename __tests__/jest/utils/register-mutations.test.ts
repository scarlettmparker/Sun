/**
 * Tests for mutation registration.
 */

// Mock the registerBlogCreateMutation function
jest.mock("~/routes/blog/create/create-blog-post", () => ({
  registerBlogCreateMutation: jest.fn(),
}));

import { registerBlogCreateMutation } from "~/routes/blog/create/create-blog-post";

describe("Mutation registration", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should register blog create mutation", async () => {
    jest.resetModules();
    await import("~/utils/register-mutations");

    expect(registerBlogCreateMutation).toHaveBeenCalledTimes(1);
  });
});
