/**
 * Tests for the stem-player data loader.
 */

import type { MockedFunction } from "vitest";
import { pageDataLoaders } from "@sun/ssr";
import "~/routes/stem-player/stem-player-data";
import { fetchListSongs } from "~/utils/api";

vi.mock("~/utils/api", () => ({
  fetchListSongs: vi.fn(),
}));

const mockFetchListSongs = fetchListSongs as MockedFunction<
  typeof fetchListSongs
>;

/**
 * Runs the registered stem-player loader and merges its slices.
 */
async function loadSongs(): Promise<Record<string, unknown>> {
  const loaders = pageDataLoaders["stem-player"] ?? [];
  const merged: Record<string, unknown> = {};
  for (const loader of loaders) {
    const result = await loader({}, {});
    if (result) {
      Object.assign(merged, result);
    }
  }
  return merged;
}

describe("Stem Player Data Loader", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns songs when the fetch succeeds", async () => {
    const mockSongs = [{ id: "1", name: "Song 1" }];
    mockFetchListSongs.mockResolvedValue({
      success: true,
      data: { stemPlayerQueries: { list: mockSongs } },
    });

    await expect(loadSongs()).resolves.toEqual({ songs: mockSongs });
    expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
  });

  it("returns an empty song list when the fetch fails", async () => {
    mockFetchListSongs.mockResolvedValue({ success: false, error: "API Error" });

    await expect(loadSongs()).resolves.toEqual({ songs: [] });
    expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
  });

  it("returns an empty song list when the fetch throws", async () => {
    mockFetchListSongs.mockRejectedValue(new Error("Network error"));

    await expect(loadSongs()).resolves.toEqual({ songs: [] });
    expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
  });

  it("returns an empty song list when the response is missing data", async () => {
    mockFetchListSongs.mockResolvedValue({ success: true, data: null });

    await expect(loadSongs()).resolves.toEqual({ songs: [] });
    expect(mockFetchListSongs).toHaveBeenCalledTimes(1);
  });
});
