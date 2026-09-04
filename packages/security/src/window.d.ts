export declare global {
  interface Window {
    /**
     * Trusted Types factory when the browser supports it.
     */
    trustedTypes?: {
      createPolicy(
        name: string,
        rules: { createHTML?: (html: string) => string },
      ): { createHTML: (html: string) => string };
    };
  }
}
