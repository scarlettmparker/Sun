/**
 * Global type definitions for tests.
 */

declare global {
  interface Window {
    __pageData__?: Record<string, unknown>;
  }
}

export {};
