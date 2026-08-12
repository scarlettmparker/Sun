import type { FastifyInstance } from "fastify";
import type { AppRuntimeStatus } from "./types";
import { getRegistry } from "./store";
import { probeStatuses, reconcile } from "./orchestrator";
import { onStatusRescan, offStatusRescan } from "./status-events";

const POLL_MS = 5_000;
const HEARTBEAT_MS = 20_000;

/**
 * Whether two statuses differ.
 */
function changed(a: AppRuntimeStatus, b: AppRuntimeStatus): boolean {
  return (
    a.up !== b.up ||
    a.port !== b.port ||
    a.managed !== b.managed ||
    a.external !== b.external ||
    a.pid !== b.pid
  );
}

/**
 * Registers the hub endpoints and kicks off a boot-time reconcile.
 */
export function registerHubRoutes(app: FastifyInstance): void {
  app.get("/hub/api/status/stream", async (request, reply) => {
    reply.hijack();
    const raw = reply.raw;
    raw.writeHead(200, {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache, no-transform",
      Connection: "keep-alive",
      "X-Accel-Buffering": "no",
    });

    let closed = false;
    let running = false;
    let known = new Map<string, AppRuntimeStatus>();

    const send = (event: string, data: unknown): void => {
      if (!closed) {
        raw.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`);
      }
    };

    const pass = async (snapshot: boolean): Promise<void> => {
      if (running) {
        return;
      }
      running = true;
      try {
        const registry = await getRegistry();
        const liveKeys = new Set(registry.apps.map((app) => app.key));
        const next = new Map<string, AppRuntimeStatus>();
        await probeStatuses(registry, (status) => {
          next.set(status.key, status);
          const previous = snapshot ? undefined : known.get(status.key);
          if (snapshot || !previous || changed(previous, status)) {
            send("status", status);
          }
        });
        for (const key of known.keys()) {
          if (!liveKeys.has(key)) {
            send("remove", { key });
          }
        }
        known = next;
      } finally {
        running = false;
      }
    };

    void pass(true);

    const pollTimer = setInterval(() => void pass(false), POLL_MS);
    const heartbeat = setInterval(() => raw.write(": ping\n\n"), HEARTBEAT_MS);

    const rescan = (): void => {
      void pass(false);
    };
    onStatusRescan(rescan);

    const teardown = (): void => {
      if (closed) {
        return;
      }
      closed = true;
      clearInterval(pollTimer);
      clearInterval(heartbeat);
      offStatusRescan(rescan);
      raw.end();
    };
    request.raw.on("close", teardown);
  });

  void (async () => {
    try {
      const registry = await getRegistry();
      await reconcile(registry);
    } catch {
      // ignore boot-time failures
    }
  })();
}
