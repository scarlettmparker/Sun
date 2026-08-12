import type { Mock } from "vitest";

export const mockGainNodes: Array<{
  gain: { setValueAtTime: Mock };
  connect: Mock;
}> = [];

export const mockAudioContext = {
  currentTime: 0,
  createGain: vi.fn(() => {
    const gainNode = {
      gain: { setValueAtTime: vi.fn() },
      connect: vi.fn(),
    };
    mockGainNodes.push(gainNode);
    return gainNode;
  }),
  createBufferSource: vi.fn(() => ({
    buffer: null,
    connect: vi.fn(),
    start: vi.fn(),
    stop: vi.fn(),
  })),
  decodeAudioData: vi.fn(),
  destination: {},
};

const MOCK_DURATION = 120;
export const mockAudioBuffer = { duration: MOCK_DURATION };

global.AudioContext = vi.fn(() => mockAudioContext) as unknown as {
  new (contextOptions?: AudioContextOptions | undefined): AudioContext;
  prototype: AudioContext;
};
global.fetch = vi.fn();
global.requestAnimationFrame = vi.fn();
global.cancelAnimationFrame = vi.fn();
