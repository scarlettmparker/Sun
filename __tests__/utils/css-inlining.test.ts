import type { Mock } from "vitest";
import { Dirent } from "fs";
import { access, readdir, readFile } from "fs/promises";
import { join, resolve } from "path";
import { inlineCss, generateCssTag } from "~/utils/css-inlining";

vi.mock("fs/promises", () => {
  const mocks = { access: vi.fn(), readdir: vi.fn(), readFile: vi.fn() };
  return { ...mocks, default: mocks };
});

vi.mock("path", () => {
  const mocks = { resolve: vi.fn(), join: vi.fn() };
  return { ...mocks, default: mocks };
});

const mockedFs = {
  access: access as Mock,
  readdir: readdir as Mock,
  readFile: readFile as Mock,
};
const mockedPath = { resolve: resolve as Mock, join: join as Mock };

describe("inlineCss", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return empty string when not in production", async () => {
    const result = await inlineCss(false, []);
    expect(result).toBe("");
  });

  it("should read and inline CSS files from src/styles in production", async () => {
    mockedPath.resolve.mockImplementation((...args) => args.join("/"));
    mockedPath.join.mockImplementation((...args) => args.join("/"));
    mockedFs.readdir.mockResolvedValue([
      "globals.css",
      "markdown.css",
      "other.txt",
    ] as unknown as Dirent[]);
    mockedFs.readFile
      .mockResolvedValueOnce("globals content")
      .mockResolvedValueOnce("markdown content");

    const result = await inlineCss(true, []);

    expect(mockedPath.resolve).toHaveBeenCalledWith("src/styles");
    expect(mockedFs.readdir).toHaveBeenCalledWith("src/styles");
    expect(mockedFs.readFile).toHaveBeenCalledWith(
      "src/styles/globals.css",
      "utf-8",
    );
    expect(mockedFs.readFile).toHaveBeenCalledWith(
      "src/styles/markdown.css",
      "utf-8",
    );
    expect(result).toBe("globals content\nmarkdown content\n");
  });

  it("should read and inline manifest CSS files in production", async () => {
    mockedPath.resolve
      .mockImplementationOnce(() => "src/styles")
      .mockImplementationOnce(() => "dist/client/assets/index.css");
    mockedFs.readdir.mockResolvedValue([]);
    mockedFs.readFile.mockResolvedValue("manifest css content");

    const result = await inlineCss(true, ["/assets/index.css"]);

    expect(mockedPath.resolve).toHaveBeenCalledWith(
      "dist/client",
      "assets/index.css",
    );
    expect(mockedFs.readFile).toHaveBeenCalledWith(
      "dist/client/assets/index.css",
      "utf-8",
    );
    expect(result).toBe("\nmanifest css content");
  });

  it("should handle errors gracefully", async () => {
    mockedPath.resolve.mockImplementation(() => "src/styles");
    mockedFs.readdir.mockRejectedValue(new Error("Read error"));
    const consoleWarnSpy = vi
      .spyOn(console, "warn")
      .mockImplementation(() => {});

    const result = await inlineCss(true, []);

    expect(consoleWarnSpy).toHaveBeenCalledWith(
      "Failed to read CSS files for inlining:",
      expect.any(Error),
    );
    expect(result).toBe("");

    consoleWarnSpy.mockRestore();
  });

  describe("generateCssTag", () => {
    it("should return inlined style tag when in production with cssContent", () => {
      const result = generateCssTag(true, "body { color: red; }", []);
      expect(result).toBe("<style>body { color: red; }</style>");
    });

    it("should return link tags when not in production and clientCss provided", () => {
      const result = generateCssTag(false, "", [
        "/assets/style.css",
        "/assets/main.css",
      ]);
      expect(result).toBe(
        '<link rel="stylesheet" href="/assets/style.css" /><link rel="stylesheet" href="/assets/main.css" />',
      );
    });

    it("should return empty string when no conditions met", () => {
      const result = generateCssTag(false, "", []);
      expect(result).toBe("");
    });

    it("should prioritize inlined style over links when in production", () => {
      const result = generateCssTag(true, "body { color: blue; }", [
        "/assets/style.css",
      ]);
      expect(result).toBe("<style>body { color: blue; }</style>");
    });
  });
});
