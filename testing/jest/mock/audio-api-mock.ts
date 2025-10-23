// AudioContext and related Web Audio API
export const mockGainNodes: Array<{
  gain: { setValueAtTime: jest.Mock };
  connect: jest.Mock;
}> = [];

export const mockAudioContext = {
  currentTime: 0,
  createGain: jest.fn(() => {
    const gainNode = {
      gain: { setValueAtTime: jest.fn() },
      connect: jest.fn(),
    };
    mockGainNodes.push(gainNode);
    return gainNode;
  }),
  createBufferSource: jest.fn(() => ({
    buffer: null,
    connect: jest.fn(),
    start: jest.fn(),
    stop: jest.fn(),
  })),
  decodeAudioData: jest.fn(),
  destination: {},
};

const MOCK_DURATION = 120;
export const mockAudioBuffer = { duration: MOCK_DURATION };

global.AudioContext = jest.fn(() => mockAudioContext) as unknown as {
  new (contextOptions?: AudioContextOptions | undefined): AudioContext;
  prototype: AudioContext;
};
global.fetch = jest.fn();
