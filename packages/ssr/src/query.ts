type QueryFetchResult =
  | { success: true; data: unknown }
  | { success: false; error?: string; statusCode?: number };

type QueryResponse = {
  json: (body: unknown) => void;
  status: (code: number) => { json: (body: unknown) => void };
};

/**
 * Generic try/catch query wrapper: runs fetchFn and writes a success or error
 * response. Framework-agnostic (Express-style res.json / res.status().json).
 *
 * @param fetchFn                Async function returning the query result.
 * @param res                    Response object with json() and status().json().
 * @param options                Optional message overrides.
 * @param options.successMessage Custom success message.
 * @param options.errorMessage   Custom error message.
 */
export async function handleQuery(
  fetchFn: () => Promise<QueryFetchResult>,
  res: QueryResponse,
  options?: { successMessage?: string; errorMessage?: string },
): Promise<void> {
  try {
    const result = await fetchFn();
    if (result.success) {
      const response: { success: true; data: unknown; message?: string } = {
        success: true,
        data: result.data,
      };
      if (options?.successMessage) {
        response.message = options.successMessage;
      }
      res.json(response);
    } else {
      const statusCode = result.statusCode ?? 500;
      const error = options?.errorMessage ?? result.error;
      res.status(statusCode).json({ success: false, error });
    }
  } catch {
    const error = options?.errorMessage ?? "Internal server error";
    res.status(500).json({ success: false, error });
  }
}

export type { QueryFetchResult, QueryResponse };
