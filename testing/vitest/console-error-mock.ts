/**
 * Silences console.error noise from React during hook tests.
 */
export const suppressConsoleErrorsFromTests = () => {
  vi.spyOn(console, "error").mockImplementation(() => {});
};

export const restoreConsoleError = () => {
  vi.restoreAllMocks();
};
