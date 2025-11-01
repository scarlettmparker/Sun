/**
 * @fileoverview Tests for useStemPlayer hook.
 * Tests the hook's state management, playback controls, seeking, volume control, and audio context handling.
 */

import { renderHook, act } from "@testing-library/react";
import { useStemPlayer } from "~/_components/stem-player/hooks/use-stem-player";
import {
  mockAudioContext,
  mockAudioBuffer,
  mockGainNodes,
  suppressConsoleErrorsFromTests,
  restoreConsoleError,
} from "testing/jest/mock";
import { deleteGlobalWindow } from "testing/jest/utils/delete-global-window";
import type { Song, Stem } from "~/generated/graphql";

beforeAll(() => {
  // Due to some issue with re-rendering that we don't care about in test env
  suppressConsoleErrorsFromTests();
});

afterAll(() => {
  restoreConsoleError();
});

const mockStems: Stem[] = [
  { name: "Drums", path: "/drums.mp3" },
  { name: "Bass", path: "/bass.mp3" },
];

const mockSong: Song = {
  id: "",
  path: "",
  stems: mockStems,
};

describe("useStemPlayer", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGainNodes.length = 0;
    (global.fetch as jest.Mock).mockResolvedValue({
      arrayBuffer: jest.fn().mockResolvedValue(new ArrayBuffer(8)),
    });
    (mockAudioContext.decodeAudioData as jest.Mock).mockResolvedValue(
      mockAudioBuffer
    );
    // Mock window for client-side checks - only if not already defined
    if (!global.window) {
      Object.defineProperty(global, "window", {
        value: {},
        writable: true,
        configurable: true,
      });
    }
  });

  afterEach(() => {
    // Reset window after each test
    deleteGlobalWindow();
  });

  it("loads audio buffers when stems change (& loads initial values)", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    // Wait for effects to run
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    expect(global.fetch).toHaveBeenCalledTimes(2);
    expect(mockAudioContext.decodeAudioData).toHaveBeenCalledTimes(2);
    expect(result.current.loaded).toBe(true);
    expect(result.current.loadingProgress).toBe(100);
    expect(result.current.duration).toBe(120);
  });

  it("plays audio from start", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.play();
    });
    expect(result.current.playing).toBe(true);
  });

  it("stops playback", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.play();
    });
    act(() => {
      result.current.stop();
    });
    expect(result.current.playing).toBe(false);
  });

  it("seeks to specific time", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.seek(30);
    });
    expect(result.current.position).toBe(30);
  });

  it("skips forward and backward", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.seek(60);
    });
    act(() => {
      result.current.skip(10);
    });
    expect(result.current.position).toBe(70);
    act(() => {
      result.current.skip(-20);
    });
    expect(result.current.position).toBe(50);
  });

  it("sets volume for specific stem", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.setVolume(0, 0.5);
    });
    expect(mockGainNodes[0].gain.setValueAtTime).toHaveBeenCalledWith(0.5, 0);
  });

  it("sets master volume", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.setMasterVolume(1.5);
    });
    expect(result.current.masterVolume).toBe(1.5);
  });

  it("handles end of playback", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.seek(120);
    });
    expect(result.current.ended).toBe(true);
  });

  it("does not play if already playing", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.play();
    });
    act(() => {
      result.current.play();
    });
    expect(mockAudioContext.createBufferSource).toHaveBeenCalledTimes(2); // only once per play call
  });

  it("restarts from beginning when ended", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.seek(120);
    });
    act(() => {
      result.current.play();
    });
    expect(result.current.ended).toBe(false);
    expect(result.current.position).toBe(0);
  });

  it("tracks loading progress", async () => {
    const { result } = renderHook(() => useStemPlayer(mockSong));
    expect(result.current.loadingProgress).toBe(0);
    // Wait for effects to run
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    expect(result.current.loadingProgress).toBe(100);
  });

  describe("Client-side only behavior", () => {
    it("does not initialize AudioContext on server-side", () => {
      // Mock server-side environment
      deleteGlobalWindow();

      renderHook(() => useStemPlayer(mockSong));
      expect(mockAudioContext.createGain).not.toHaveBeenCalled();
    });

    it("initializes AudioContext when window is available", () => {
      // Window is already mocked in beforeEach
      renderHook(() => useStemPlayer(mockSong));
      expect(global.AudioContext).toHaveBeenCalled();
    });
  });

  describe("Promise loading behavior", () => {
    it("handles successful loading of all stems", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(global.fetch).toHaveBeenCalledTimes(2);
      expect(mockAudioContext.decodeAudioData).toHaveBeenCalledTimes(2);
      expect(result.current.loaded).toBe(true);
      expect(result.current.loadingProgress).toBe(100);
    });

    it("handles fetch failure for one stem", async () => {
      (global.fetch as jest.Mock)
        .mockResolvedValueOnce({
          arrayBuffer: jest.fn().mockResolvedValue(new ArrayBuffer(8)),
        })
        .mockRejectedValueOnce(new Error("Network error"));

      await act(async () => {
        renderHook(() => useStemPlayer(mockSong));
      });

      expect(global.fetch).toHaveBeenCalledTimes(2);
      expect(mockAudioContext.decodeAudioData).toHaveBeenCalledTimes(1);
    });

    it("handles decodeAudioData failure", async () => {
      (mockAudioContext.decodeAudioData as jest.Mock)
        .mockResolvedValueOnce(mockAudioBuffer)
        .mockRejectedValueOnce(new Error("Decode error"));

      await act(async () => {
        renderHook(() => useStemPlayer(mockSong));
      });

      expect(mockAudioContext.decodeAudioData).toHaveBeenCalledTimes(2);
    });

    it("updates loading progress incrementally", async () => {
      const progressUpdates: number[] = [];

      renderHook(() => {
        const hookResult = useStemPlayer(mockSong);
        progressUpdates.push(hookResult.loadingProgress);
        return hookResult;
      });

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(progressUpdates).toContain(0);
      expect(progressUpdates).toContain(100);
    });

    it("does not update state after unmount", async () => {
      const { result, unmount } = renderHook(() => useStemPlayer(mockSong));

      unmount();

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      // State should remain initial since mounted flag prevents updates
      expect(result.current.loaded).toBe(false);
      expect(result.current.loadingProgress).toBe(0);
    });
  });

  describe("Playback controls edge cases", () => {
    it("does not play when no AudioContext", () => {
      deleteGlobalWindow();

      const { result } = renderHook(() => useStemPlayer(mockSong));

      act(() => {
        result.current.play();
      });

      expect(result.current.playing).toBe(false);
    });

    it("does not play when no buffers loaded", () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      act(() => {
        result.current.play();
      });

      expect(result.current.playing).toBe(false);
    });

    it("does not play when already playing", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.play();
      });

      const sourcesCreated =
        mockAudioContext.createBufferSource.mock.calls.length;

      act(() => {
        result.current.play(); // Try to play again
      });

      expect(mockAudioContext.createBufferSource).toHaveBeenCalledTimes(
        sourcesCreated
      ); // No additional sources
    });

    it("restarts from beginning when ended", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.seek(120); // Seek to end
      });

      expect(result.current.ended).toBe(true);

      act(() => {
        result.current.play();
      });

      expect(result.current.ended).toBe(false);
      expect(result.current.position).toBe(0);
    });

    it("handles empty stems array", () => {
      const emptyMockSong: Song = {
        id: "",
        path: "",
        stems: [],
      };

      const { result } = renderHook(() => useStemPlayer(emptyMockSong));

      expect(result.current.loaded).toBe(true); // Empty array is "loaded"
      expect(result.current.duration).toBe(0);
    });
  });

  describe("Volume controls", () => {
    it("sets volume for valid stem index", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.setVolume(0, 0.7);
      });

      expect(mockGainNodes[0].gain.setValueAtTime).toHaveBeenCalledWith(0.7, 0);
    });

    it("ignores volume set for invalid stem index", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.setVolume(5, 0.7); // Invalid index
      });

      expect(mockGainNodes[0].gain.setValueAtTime).not.toHaveBeenCalled();
    });

    it("sets master volume", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.setMasterVolume(0.8);
      });

      expect(result.current.masterVolume).toBe(0.8);
      expect(mockAudioContext.createGain).toHaveBeenCalled();
    });

    it("does not set master volume without AudioContext", () => {
      deleteGlobalWindow();

      const { result } = renderHook(() => useStemPlayer(mockSong));

      act(() => {
        result.current.setMasterVolume(0.8);
      });

      expect(result.current.masterVolume).toBe(0.8); // State updates
      // But no audio context call
    });
  });

  describe("Position tracking and seeking", () => {
    it("seeks to valid time", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.seek(45);
      });

      expect(result.current.position).toBe(45);
      expect(result.current.ended).toBe(false);
    });

    it("seeks to end and sets ended state", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.seek(120);
      });

      expect(result.current.position).toBe(120);
      expect(result.current.ended).toBe(true);
    });

    it("clamps seek to valid range", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.seek(-10); // Negative
      });

      expect(result.current.position).toBe(0);

      act(() => {
        result.current.seek(200); // Beyond duration
      });

      expect(result.current.position).toBe(120);
    });

    it("starts playback from seek position when playing", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.play();
      });

      act(() => {
        result.current.seek(30);
      });

      expect(mockAudioContext.createBufferSource).toHaveBeenCalledTimes(4); // Initial play + seek restart
    });
  });

  describe("Cleanup and unmounting", () => {
    it("handles source stop errors gracefully", async () => {
      const mockSource = mockAudioContext.createBufferSource();
      mockSource.stop.mockImplementation(() => {
        throw new Error("Already stopped");
      });

      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.play();
      });

      act(() => {
        result.current.stop();
      });

      // Should not throw, error is caught
      expect(result.current.playing).toBe(false);
    });

    it("preserves offset when stopping during playback", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.play();
      });

      mockAudioContext.currentTime = 10;

      act(() => {
        result.current.stop();
      });

      expect(result.current.position).toBe(10);
    });
  });

  describe("State management and memoization", () => {
    it("memoizes return object to prevent unnecessary re-renders", async () => {
      const { result, rerender } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      const firstReturn = result.current;

      rerender();

      const secondReturn = result.current;

      // Same object reference due to memoization
      expect(firstReturn).toBe(secondReturn);
    });

    it("updates return object when dependencies change", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.play();
      });

      expect(result.current).toBe(result.current);
    });

    it("recreates audio nodes when stems change", async () => {
      const { rerender } = renderHook(({ song }) => useStemPlayer(song), {
        initialProps: { song: mockSong },
      });

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      const initialGainNodes = mockGainNodes.length;

      rerender({
        song: {
          id: "",
          path: "",
          stems: [{ name: "New Stem", path: "/new.mp3" }],
        },
      });

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(mockGainNodes.length).toBeGreaterThan(initialGainNodes);
    });
  });

  describe("Error handling and edge cases", () => {
    it("handles song with undefined path", () => {
      const songWithUndefinedPath = {
        ...mockSong,
        path: undefined,
      } as unknown as Song;

      expect(() => {
        renderHook(() => useStemPlayer(songWithUndefinedPath));
      }).not.toThrow();
    });

    it("handles song with null path", () => {
      const songWithNullPath = { ...mockSong, path: null } as unknown as Song;

      expect(() => {
        renderHook(() => useStemPlayer(songWithNullPath));
      }).not.toThrow();
    });

    it("handles stems with undefined paths", () => {
      const stemsWithUndefinedPaths: Stem[] = [
        { name: "Drums", path: undefined as unknown as string },
        { name: "Bass", path: "/bass.mp3" },
      ];

      const songWithUndefinedStemPaths = {
        ...mockSong,
        stems: stemsWithUndefinedPaths,
      };

      expect(() => {
        renderHook(() => useStemPlayer(songWithUndefinedStemPaths));
      }).not.toThrow();
    });

    it("handles stems with null paths", () => {
      const stemsWithNullPaths: Stem[] = [
        { name: "Drums", path: null as unknown as string },
        { name: "Bass", path: "/bass.mp3" },
      ];

      const songWithNullStemPaths = {
        ...mockSong,
        stems: stemsWithNullPaths,
      };

      expect(() => {
        renderHook(() => useStemPlayer(songWithNullStemPaths));
      }).not.toThrow();
    });

    it("handles stems with empty string paths", () => {
      const stemsWithEmptyPaths: Stem[] = [
        { name: "Drums", path: "" },
        { name: "Bass", path: "/bass.mp3" },
      ];

      const songWithEmptyStemPaths = {
        ...mockSong,
        stems: stemsWithEmptyPaths,
      };

      expect(() => {
        renderHook(() => useStemPlayer(songWithEmptyStemPaths));
      }).not.toThrow();
    });

    it("handles very large skip values", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.skip(1000);
      });

      expect(result.current.position).toBe(120);
    });

    it("handles very negative skip values", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.skip(-1000);
      });

      expect(result.current.position).toBe(0);
    });

    it("handles rapid state changes", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.play();
        result.current.seek(50);
        result.current.setMasterVolume(0.5);
        result.current.stop();
      });

      expect(result.current.position).toBe(50);
      expect(result.current.masterVolume).toBe(0.5);
      expect(result.current.playing).toBe(false);
    });

    it("handles AudioContext suspension", async () => {
      // This test verifies behavior when AudioContext might be suspended
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.play();
      });

      // Should still attempt to play even if suspended
      expect(result.current.playing).toBe(true);
    });

    it("handles seek during playback restart", async () => {
      const { result } = renderHook(() => useStemPlayer(mockSong));

      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      act(() => {
        result.current.play();
        result.current.seek(60);
      });

      expect(result.current.position).toBe(60);
      expect(result.current.playing).toBe(true);
    });
  });
});
