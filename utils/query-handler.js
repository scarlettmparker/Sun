/**
 * Generic query handler utility for API routes.
 * Handles try-catch, error codes, and response formatting.
 * Allows overriding success and error messages.
 *
 * @param {Function} fetchFn - The async function to fetch data (e.g., fetchListSongs).
 * @param {Object} res - Express response object.
 * @param {Object} options - Optional overrides.
 * @param {string} options.successMessage - Custom message for successful responses.
 * @param {string} options.errorMessage - Custom message for error responses.
 */
export async function handleQuery(fetchFn, res, options = {}) {
  try {
    const result = await fetchFn();

    if (result.success) {
      const response = {
        success: true,
        data: result.data,
      };
      if (options.successMessage) {
        response.message = options.successMessage;
      }
      res.json(response);
    } else {
      const statusCode = result.statusCode || 500;
      const error = options.errorMessage || result.error;
      res.status(statusCode).json({
        success: false,
        error,
      });
    }
  } catch (_error) {
    const error = options.errorMessage || "Internal server error";
    res.status(500).json({
      success: false,
      error,
    });
  }
}
