export * from "./console-error-mock";
export * from "./audio-api-mock";
// css-module-mock is already handled via moduleNameMapper

// eslint-disable-next-line @typescript-eslint/no-extraneous-class
class ResizeObserverMock {
  observe() { /* noop */ }
  unobserve() { /* noop */ }
  disconnect() { /* noop */ }
}

global.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver;
