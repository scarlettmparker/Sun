import { createElement, type ComponentType } from "preact/compat";

/**
 * A component whose module can be loaded before hydration.
 */
type LazyComponent = ComponentType<Record<string, unknown>>;

const preloaders = new Set<() => Promise<unknown>>();

/**
 * Creates a lazy component whose module can be loaded before hydration.
 *
 * @param loader Imports the component module.
 * @returns The lazy component.
 */
export function preloadableLazy(
  loader: () => Promise<{ default: LazyComponent }>,
): LazyComponent {
  let pending: Promise<unknown> | null = null;
  let component: LazyComponent | null = null;
  let error: unknown = null;
  let resolved = false;

  const preload = () => {
    if (!pending) {
      pending = loader().then(
        (module) => {
          component = module.default;
          resolved = true;
        },
        (reason) => {
          error = reason;
          resolved = true;
        },
      );
    }
    return pending;
  };

  preloaders.add(preload);

  const Lazy = (props: Record<string, unknown>) => {
    if (error) throw error;
    if (!resolved) throw preload();
    return component ? createElement(component, props) : null;
  };

  Lazy.displayName = "Lazy";

  return Lazy;
}

/**
 * Loads every registered lazy component module.
 */
export function preloadLazyComponents(): Promise<unknown[]> {
  return Promise.all([...preloaders].map((preload) => preload()));
}
