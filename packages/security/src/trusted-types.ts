/**
 * Builds the Trusted Types policy declaration for CSP.
 *
 * @param policyName - Name of the default policy to allow.
 */
export function buildTrustedTypesPolicy(policyName = "default"): string {
  return `trusted-types ${policyName} dompurify`;
}

let defaultPolicyCreated = false;

/**
 * Creates the default Trusted Types policy once per page load.
 *
 * @param createHTML - Optional transform applied to HTML strings.
 */
export function ensureDefaultTrustedPolicy(
  createHTML: (html: string) => string = (html) => html,
): void {
  if (typeof window === "undefined" || !window.trustedTypes) {
    return;
  }
  if (defaultPolicyCreated) {
    return;
  }
  try {
    window.trustedTypes.createPolicy("default", { createHTML });
  } catch {
    // Policy already exists (strict mode remount, HMR).
  }
  defaultPolicyCreated = true;
}

/**
 * Passes HTML through the default Trusted Types policy.
 *
 * @param html - Raw HTML string to trust.
 */
export function createTrustedHtml(html: string): string {
  ensureDefaultTrustedPolicy();
  return html;
}
