/**
 * Fastify middleware that blocks requests from IPs not on the backend's
 * whitelist. The whitelist is fetched via REST (see IpWhitelistController) so
 * the check is available before any GraphQL initialisation.
 */

import { ipMatchesCidr } from "@sun/utils/cidr";
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";

export type IpWhitelistConfig = {
  /**
   * Backend base URL.
   */
  backendUrl: string;
  /**
   * Path prefixes that bypass the IP check.
   */
  exemptPaths?: string[];
  /**
   * Whitelist refresh interval in ms.
   *
   * @default 60000
   */
  refreshIntervalMs?: number;
};

/**
 * Registers a Fastify onRequest hook that blocks non-whitelisted IPs.
 */
export function registerIpWhitelist(
  app: FastifyInstance,
  config: IpWhitelistConfig,
  clientId?: string,
): void {
  let patterns: string[] = [];
  let bypass = false;

  const fetch = async () => {
    try {
      const url = `${config.backendUrl}/api/public/ip-whitelist`;
      const headers: Record<string, string> = {};
      if (clientId) {
        headers["X-Client-Id"] = clientId;
      }
      const res = await fetch(url, { headers });
      if (res.ok) {
        const json = (await res.json()) as {
          patterns: string[];
          bypass: boolean;
        };
        patterns = json.patterns;
        bypass = json.bypass;
      }
    } catch {
      /* keep previous state on failure */
    }
  };

  fetch();
  setInterval(fetch, config.refreshIntervalMs ?? 60000);

  app.addHook(
    "onRequest",
    async (request: FastifyRequest, reply: FastifyReply) => {
      if (bypass) return;
      if (!patterns.length) return;

      const path = request.url.split("?")[0];
      if (config.exemptPaths?.some((p) => path.startsWith(p))) return;

      const ip = request.ip;
      const allowed = patterns.some((p) => ipMatchesCidr(ip, p));
      if (!allowed) {
        reply.code(404).send();
      }
    },
  );
}
