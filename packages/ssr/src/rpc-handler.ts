import { pageDataLoaders } from "./page-data";

type RpcRequest = {
  body?: { pattern?: string; params?: Record<string, unknown> };
};
type RpcReply = {
  send: (payload: unknown) => void;
  status: (code: number) => { send: (payload: unknown) => void };
};

export function pageDataRpcHandler() {
  return async (request: RpcRequest, reply: RpcReply): Promise<void> => {
    const { pattern, params } = request.body || {};
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
