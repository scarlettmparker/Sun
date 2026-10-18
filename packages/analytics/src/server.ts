import { createHash } from "node:crypto";
import { PostHog } from "posthog-node";

type EventProperties = Record<string, unknown>;

export type PageViewInput = {
  /**
   * App name reported on the event.
   */
  app: string;
  /**
   * Absolute URL of the rendered page.
   */
  url: string;
  /**
   * Pathname, when it differs from the url.
   */
  path?: string;
  /**
   * Account id, when already resolved by the caller.
   */
  userId?: string | null;
  /**
   * Cookie holding the auth JWT.
   */
  authCookie?: string;
  /**
   * Raw Cookie header from the request.
   */
  cookie?: string;
  /**
   * Client ip.
   */
  ip?: string;
  /**
   * Client user-agent.
   */
  userAgent?: string;
  /**
   * Referring page.
   */
  referrer?: string;
  /**
   * Consent cookie that must read `accepted` before capturing.
   */
  consentCookie?: string;
};

export type SignInInput = {
  /**
   * App name reported on the event.
   */
  app: string;
  /**
   * Auth JWT from the login response.
   */
  token?: string;
  /**
   * Account id, when already decoded.
   */
  accountId?: string;
  /**
   * Client ip.
   */
  ip?: string;
  /**
   * Client user-agent.
   */
  userAgent?: string;
};

export type SignOutInput = {
  /**
   * App name reported on the event.
   */
  app: string;
  /**
   * Auth JWT being revoked.
   */
  token?: string;
  /**
   * Client ip.
   */
  ip?: string;
  /**
   * Client user-agent.
   */
  userAgent?: string;
};

let client: PostHog | null = null;
let unavailable = false;

/**
 * Creates the PostHog client from the environment on first use. Returns null
 * when no api key is configured.
 *
 * @returns The shared client, or null.
 */
function getClient(): PostHog | null {
  if (client) return client;
  if (unavailable) return null;
  const apiKey = process.env.POSTHOG_API_KEY;
  if (!apiKey) {
    unavailable = true;
    return null;
  }
  client = new PostHog(apiKey, {
    host: process.env.POSTHOG_HOST || "https://eu.i.posthog.com",
    flushAt: 20,
    flushInterval: 10_000,
  });
  return client;
}

/**
 * Reads a named cookie from a raw Cookie header.
 *
 * @param header Raw Cookie header, or undefined.
 * @param name Cookie name.
 * @returns The decoded value, or undefined.
 */
function readCookie(header: string | undefined, name: string): string | undefined {
  if (!header) return undefined;
  for (const part of header.split(/;\s*/)) {
    const index = part.indexOf("=");
    if (index < 0) continue;
    if (part.slice(0, index).trim() === name) {
      return decodeURIComponent(part.slice(index + 1));
    }
  }
  return undefined;
}

/**
 * Reads the account id from a JWT's `sub` claim. The signature is not checked
 * because analytics only needs a stable id.
 *
 * @param token Raw JWT, or undefined.
 * @returns The account id, or null.
 */
export function accountIdFromToken(token: string | undefined): string | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(
      Buffer.from(parts[1], "base64url").toString("utf8"),
    ) as { sub?: unknown };
    return typeof payload.sub === "string" && payload.sub ? payload.sub : null;
  } catch {
    return null;
  }
}

/**
 * Derives a stable anonymous id from the client ip and user-agent.
 *
 * @param ip Client ip.
 * @param userAgent Client user-agent.
 * @returns A 32-character hex id.
 */
function anonymousId(ip: string | undefined, userAgent: string | undefined): string {
  return createHash("sha256")
    .update(`${ip ?? ""}|${userAgent ?? ""}`)
    .digest("hex")
    .slice(0, 32);
}

/**
 * Builds the PostHog distinct id, preferring the account id from a JWT.
 *
 * @param token Auth JWT, or undefined.
 * @param ip Client ip.
 * @param userAgent Client user-agent.
 * @returns `user:<accountId>` when the token decodes, else `anon:<hash>`.
 */
export function distinctIdFor(
  token: string | undefined,
  ip: string | undefined,
  userAgent: string | undefined,
): string {
  const accountId = accountIdFromToken(token);
  return accountId ? `user:${accountId}` : `anon:${anonymousId(ip, userAgent)}`;
}

/**
 * Captures a named event for a distinct id.
 *
 * @param event Event name.
 * @param distinctId PostHog distinct id.
 * @param properties Event properties.
 */
export function captureEvent(
  event: string,
  distinctId: string,
  properties?: EventProperties,
): void {
  getClient()?.capture({ distinctId, event, properties });
}

/**
 * Attaches person properties to a distinct id.
 *
 * @param distinctId PostHog distinct id.
 * @param properties Person properties.
 */
export function identifyUser(
  distinctId: string,
  properties?: EventProperties,
): void {
  getClient()?.identify({ distinctId, properties });
}

/**
 * Captures a `$pageview`. Skips when analytics is unconfigured, when the url is
 * missing, or when consent is required and not granted.
 *
 * @param input Request and app details.
 */
export function trackPageView(input: PageViewInput): void {
  const analytics = getClient();
  if (!analytics || !input.url) return;
  if (input.consentCookie) {
    const consent = readCookie(input.cookie, input.consentCookie);
    if (consent !== "accepted") return;
  }
  const token = input.authCookie
    ? readCookie(input.cookie, input.authCookie)
    : undefined;
  const accountId = input.userId ?? accountIdFromToken(token);
  const distinctId = accountId
    ? `user:${accountId}`
    : `anon:${anonymousId(input.ip, input.userAgent)}`;

  analytics.capture({
    distinctId,
    event: "$pageview",
    properties: {
      app: input.app,
      $current_url: input.url,
      $pathname: input.path ?? input.url,
      $referrer: input.referrer,
      $ip: input.ip,
      $useragent: input.userAgent,
    },
  });
}

/**
 * Records a successful sign-in: identifies the account, links the anonymous id
 * to it, and captures a `sign_in` event.
 *
 * @param input App name, auth token, and request details.
 */
export function trackSignIn(input: SignInInput): void {
  const analytics = getClient();
  if (!analytics) return;
  const accountId = input.accountId ?? accountIdFromToken(input.token);
  if (!accountId) return;
  const distinctId = `user:${accountId}`;
  analytics.identify({ distinctId, properties: { app: input.app } });
  analytics.alias({
    distinctId: `anon:${anonymousId(input.ip, input.userAgent)}`,
    alias: distinctId,
  });
  analytics.capture({
    distinctId,
    event: "sign_in",
    properties: { app: input.app },
  });
}

/**
 * Records a sign-out for the account carried by the token.
 *
 * @param input App name, auth token, and request details.
 */
export function trackSignOut(input: SignOutInput): void {
  const analytics = getClient();
  if (!analytics) return;
  analytics.capture({
    distinctId: distinctIdFor(input.token, input.ip, input.userAgent),
    event: "sign_out",
    properties: { app: input.app },
  });
}

/**
 * Flushes buffered events and stops the client. Call during shutdown.
 */
export async function shutdownAnalytics(): Promise<void> {
  if (!client) return;
  const active = client;
  client = null;
  await active.shutdown();
}
