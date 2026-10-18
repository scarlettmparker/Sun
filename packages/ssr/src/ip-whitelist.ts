/**
 * Fastify middleware that blocks requests from IPs not on the backend's
 * whitelist. The whitelist is fetched via REST (see IpWhitelistController) so
 * the check is available before any GraphQL initialisation.
 */

import { ipMatchesCidr } from "@sun/utils/cidr";
import type { SunApp, SunReply, SunRequest } from "./server-types";

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
 * Registers an onRequest hook that blocks non-whitelisted IPs.
 */
export function registerIpWhitelist(
  app: SunApp,
  config: IpWhitelistConfig,
  clientId?: string,
): void {
  let patterns: string[] = [];
  let bypass = false;

  const refreshWhitelist = async () => {
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

  refreshWhitelist();
  setInterval(refreshWhitelist, config.refreshIntervalMs ?? 60000);

  app.addHook("onRequest", async (request: SunRequest, reply: SunReply) => {
    if (bypass) return;
    if (!patterns.length) return;

    const path = request.url.split("?")[0];
    if (config.exemptPaths?.some((p) => path.startsWith(p))) return;

    const ip = request.ip;
    const allowed = patterns.some((p) => ipMatchesCidr(ip, p));
    if (!allowed) {
      reply.code(404).send();
    }
  });
}
