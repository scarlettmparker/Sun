/**
 * @fileoverview Tests for useStemPlayer hook.
 * Tests the hook's state management, playback controls, seeking, volume control, and audio context handling.
 */

import { renderHook, act } from "@testing-library/react";
import { useStemPlayer } from "~/_components/stem-player/hooks/use-stem-player";
import { Stem } from "~/_components/stem-player/types/stem";
import {
  mockAudioContext,
  mockAudioBuffer,
  mockGainNodes,
} from "testing/jest/mock";

describe("useStemPlayer", () => {
  const mockStems: Stem[] = [
    { name: "Drums", url: "/drums.mp3" },
    { name: "Bass", url: "/bass.mp3" },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    mockGainNodes.length = 0;
    (global.fetch as jest.Mock).mockResolvedValue({
      arrayBuffer: jest.fn().mockResolvedValue(new ArrayBuffer(8)),
    });
    (mockAudioContext.decodeAudioData as jest.Mock).mockResolvedValue(
      mockAudioBuffer
    );
  });

  it("loads audio buffers when stems change (& loads initial values)", async () => {
    const { result } = renderHook(() => useStemPlayer(mockStems));
    // Wait for effects to run
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    expect(global.fetch).toHaveBeenCalledTimes(2);
    expect(mockAudioContext.decodeAudioData).toHaveBeenCalledTimes(2);
    expect(result.current.loaded).toBe(true);
    expect(result.current.duration).toBe(120);
  });

  it("plays audio from start", async () => {
    const { result } = renderHook(() => useStemPlayer(mockStems));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.play();
    });
    expect(result.current.playing).toBe(true);
  });

  it("stops playback", async () => {
    const { result } = renderHook(() => useStemPlayer(mockStems));
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
    const { result } = renderHook(() => useStemPlayer(mockStems));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.seek(30);
    });
    expect(result.current.position).toBe(30);
  });

  it("skips forward and backward", async () => {
    const { result } = renderHook(() => useStemPlayer(mockStems));
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
    const { result } = renderHook(() => useStemPlayer(mockStems));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.setVolume(0, 0.5);
    });
    expect(mockGainNodes[0].gain.setValueAtTime).toHaveBeenCalledWith(0.5, 0);
  });

  it("handles end of playback", async () => {
    const { result } = renderHook(() => useStemPlayer(mockStems));
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    act(() => {
      result.current.seek(120);
    });
    expect(result.current.ended).toBe(true);
  });

  it("does not play if already playing", async () => {
    const { result } = renderHook(() => useStemPlayer(mockStems));
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
    const { result } = renderHook(() => useStemPlayer(mockStems));
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
});
