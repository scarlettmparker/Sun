const CSRF_COOKIE = "csrf_token";
const CSRF_HEADER = "x-csrf-token";

function readCookie(name: string): string | undefined {
  if (typeof document === "undefined" || !document.cookie) return undefined;
  for (const part of document.cookie.split(/;\s*/)) {
    const index = part.indexOf("=");
    if (index < 0) continue;
    if (part.slice(0, index).trim() === name) {
      return decodeURIComponent(part.slice(index + 1));
    }
  }
  return undefined;
}

/**
 * Sends a first-party pageview beacon for a client-side navigation. No-op
 * outside the browser.
 *
 * @param url Absolute URL of the rendered page.
 */
export function trackPageView(url?: string): void {
  if (typeof window === "undefined") return;
  const body = JSON.stringify({ url: url ?? window.location.href });
  void fetch("/__analytics", {
    method: "POST",
    credentials: "same-origin",
    keepalive: true,
    headers: {
      "Content-Type": "application/json",
      [CSRF_HEADER]: readCookie(CSRF_COOKIE) ?? "",
    },
    body,
  }).catch(() => {});
}
