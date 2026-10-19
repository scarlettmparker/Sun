import { pageDataLoaders } from "./page-data";
import type { SunReply, SunRequest } from "./server-types";

type PageDataRequestBody = {
  pattern?: string;
  params?: Record<string, unknown>;
};

/**
 * Route handler factory for the /__page-data RPC. Runs the registered
 * server-side loaders for the requested pattern and returns the merged data.
 */
export function pageDataRpcHandler() {
  return async (request: SunRequest, reply: SunReply): Promise<void> => {
    const { pattern, params } = (request.body as PageDataRequestBody | undefined) ?? {};
    const loaders = pageDataLoaders[pattern ?? ""];
    if (!loaders || !loaders.length) {
      reply.send({ data: null });
      return;
    }
    try {
      const results = await Promise.all(
        loaders.map((l) => l(params, { cookie: request.headers.cookie })),
      );
      const merged: Record<string, unknown> = {};
      for (const r of results) {
        if (r && typeof r === "object") Object.assign(merged, r);
      }
      reply.send({ data: merged });
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      reply.status(500).send({ data: null, error: msg });
    }
  };
}
