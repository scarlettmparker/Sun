/**
 * Global type definitions for tests.
 */

declare global {
  interface Window {
    __locale__?: string;
    __translations__?: Record<string, unknown>;
    __pageData__?: Record<string, unknown>;
  }
}

export {};
