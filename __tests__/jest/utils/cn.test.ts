/**
 * Tests for cn utility function.
 */

import { cn } from "~/utils/cn";

describe("cn", () => {
  it("should join multiple class names with spaces", () => {
    expect(cn("class1", "class2", "class3")).toBe("class1 class2 class3");
  });

  it("should filter out undefined values", () => {
    expect(cn("class1", undefined, "class2")).toBe("class1 class2");
  });

  it("should filter out null values", () => {
    expect(cn("class1", null, "class2")).toBe("class1 class2");
  });

  it("should filter out false values", () => {
    expect(cn("class1", false, "class2")).toBe("class1 class2");
  });

  it("should filter out empty strings", () => {
    expect(cn("class1", "", "class2")).toBe("class1 class2");
  });

  it("should handle all falsy values", () => {
    expect(cn(undefined, null, false, "")).toBe("");
  });

  it("should handle single class name", () => {
    expect(cn("single")).toBe("single");
  });

  it("should handle no arguments", () => {
    expect(cn()).toBe("");
  });

  it("should handle mixed falsy and truthy values", () => {
    expect(cn("a", undefined, "b", null, "c", false, "", "d")).toBe("a b c d");
  });

  it("should handle only falsy values resulting in empty string", () => {
    expect(cn("", undefined, null, false)).toBe("");
  });

  it("should preserve spaces in class names", () => {
    expect(cn("class with spaces", "another")).toBe(
      "class with spaces another"
    );
  });
});
