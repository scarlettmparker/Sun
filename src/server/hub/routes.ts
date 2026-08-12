import type { FastifyInstance } from "fastify";
import { getRegistry } from "./store";
import { allStatuses, reconcile } from "./orchestrator";

/**
 * Registers the read-only hub endpoints and kicks off a boot-time reconcile.
 */
export function registerHubRoutes(app: FastifyInstance): void {
  app.get("/hub/api/status", async (_request, reply) => {
    const registry = await getRegistry();
    return reply.send(await allStatuses(registry));
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
