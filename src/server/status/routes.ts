import type { FastifyInstance } from "fastify";
import { buildReport } from "./report";

/**
 * Registers the status JSON endpoint.
 */
export function registerStatusRoutes(app: FastifyInstance): void {
  app.get("/status.json", async (_request, reply) => {
    return reply.send(await buildReport());
  });
}
