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

export const mockAudioBuffer = { duration: 120 };

// Setup global mocks
global.AudioContext = jest.fn(() => mockAudioContext) as any;
global.fetch = jest.fn();
