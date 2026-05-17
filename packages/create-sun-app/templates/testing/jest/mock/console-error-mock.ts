const originalConsoleError = console.error;

/**
 * Only console error if inside component.
 */
export const suppressConsoleErrorsFromTests = () => {
  jest.spyOn(console, "error").mockImplementation((...args) => {
    const maybeError = args.find((arg) => arg instanceof Error);

    // Check if stack includes runTest (means it comes from component)
    if (maybeError && typeof maybeError.stack === "string") {
      const stack = maybeError.stack;
      if (stack.includes("runTest")) {
        originalConsoleError(...args);
      }
    }
  });
};

export const restoreConsoleError = () => {
  jest.restoreAllMocks();
};
