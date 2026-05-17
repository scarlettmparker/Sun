/**
 * @function resolve
 * Handles `.css` imports during SSR by short-circuiting resolution so Node
 * doesn't attempt to load CSS files as JS modules in an SSR context.
 */
export async function resolve(specifier, context, defaultResolve) {
  if (specifier.endsWith(".css")) {
    return {
      shortCircuit: true,
      url: new URL(specifier, context.parentURL).href,
    };
  }
  return defaultResolve(specifier, context);
}

/**
 * @function load
 * Returns an empty module for `.css` imports during SSR to avoid errors when
 * server code imports stylesheets.
 */
export async function load(url, context, defaultLoad) {
  if (url.endsWith(".css")) {
    return {
      format: "module",
      shortCircuit: true,
      source: "export default {};",
    };
  }
  return defaultLoad(url, context);
}
