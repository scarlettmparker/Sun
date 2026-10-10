export * from "./console-error-mock";
export * from "./audio-api-mock";

// eslint-disable-next-line @typescript-eslint/no-extraneous-class
class ResizeObserverMock {
  observe() {
  }
  unobserve() {
  }
  disconnect() {
  }
}

global.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver;
