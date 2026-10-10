/**
 * Guards the shared security header builders against regressions.
 */

import { buildCspHeader } from "../src/csp";
import { buildHstsHeader } from "../src/hsts";
import { buildSecurityHeaders } from "../src/headers";

const NONCE = "dGVzdG5vbmNlMTIzNDU2";

describe("buildSecurityHeaders", () => {
  it("emits each header exactly once", () => {
    const entries = Object.entries(
      buildSecurityHeaders({ isProduction: true, nonce: NONCE }),
    );
    const names = entries.map(([key]) => key);

    expect(new Set(names).size).toBe(entries.length);
  });

  it("pins HSTS to the one-year preloaded value", () => {
    const headers = buildSecurityHeaders({ isProduction: true, nonce: NONCE });

    expect(headers["Strict-Transport-Security"]).toBe(
      "max-age=31536000; includeSubDomains; preload",
    );
    expect(buildHstsHeader()).toBe(headers["Strict-Transport-Security"]);
  });

  it("denies framing at both layers", () => {
    const headers = buildSecurityHeaders({ isProduction: true, nonce: NONCE });

    expect(headers["X-Frame-Options"]).toBe("DENY");
    expect(headers["Content-Security-Policy"]).toContain(
      "frame-ancestors 'none'",
    );
  });

  it("binds inline scripts to the per-request nonce", () => {
    const headers = buildSecurityHeaders({ isProduction: true, nonce: NONCE });

    expect(headers["Content-Security-Policy"]).toContain(`'nonce-${NONCE}'`);
    expect(buildCspHeader(NONCE)).toContain(`'nonce-${NONCE}'`);
  });

  it("keeps connect-src to known origins", () => {
    const headers = buildSecurityHeaders({ isProduction: true, nonce: NONCE });
    const connect =
      headers["Content-Security-Policy"].split(";").find((directive) =>
        directive.trim().startsWith("connect-src"),
      ) ?? "";

    expect(connect).toContain("https://*.posthog.com");
    expect(connect).toContain("https://*.scarlettparker.co.uk");
    expect(connect).not.toMatch(/(^|\s)https:(\s|;|$)/);
  });
});
