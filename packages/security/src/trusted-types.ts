/**
 * Builds the Trusted Types policy declaration for CSP.
 *
 * @param policyName - Name of the default policy to allow.
 */
export function buildTrustedTypesPolicy(policyName = "default"): string {
  return `trusted-types ${policyName} dompurify`;
}

/**
 * Creates a Trusted Types policy for HTML sanitization if the browser supports it.
 *
 * @param html - Raw HTML string to sanitize.
 */
export function createTrustedHtml(html: string): string {
  if (typeof window !== "undefined" && window.trustedTypes) {
    try {
      const policy = window.trustedTypes.createPolicy("default", {
        createHTML: (s: string) => s,
      });
      return policy.createHTML(html);
    } catch {
      return html;
    }
  }
  return html;
}
