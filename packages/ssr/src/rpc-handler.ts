import type { FastifyRequest, FastifyReply } from "fastify";
import { pageDataLoaders } from "./page-data";

type PageDataRequestBody = {
  pattern?: string;
  params?: Record<string, unknown>;
};

/**
 * Fastify route handler factory for the /__page-data RPC. Runs the registered
 * server-side loaders for the requested pattern and returns the merged data.
 */
export function pageDataRpcHandler() {
  return async (
    request: FastifyRequest,
    reply: FastifyReply,
  ): Promise<void> => {
    const { pattern, params } =
      (request.body as PageDataRequestBody | undefined) ?? {};
    const loaders = pageDataLoaders[pattern ?? ""];
    if (!loaders || !loaders.length) {
      reply.send({ data: null });
      return;
    }
    try {
      const results = await Promise.all(loaders.map((l) => l(params)));
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
