import { useEffect, useMemo, useState } from "preact/hooks";
import type { i18n as I18nInstance, TOptions } from "i18next";

/**
 * A namespace-bound translation function.
 */
export type Translator = (key: string, options?: TOptions) => string;

/**
 * The slice of an i18next instance this package relies on.
 */
type I18nLike = {
  language: string;
  t: (key: string, options?: TOptions) => unknown;
  on: (event: string, listener: () => void) => unknown;
};

let current: I18nLike | null = null;
const listeners = new Set<() => void>();
const attached = new WeakSet<I18nLike>();

/**
 * Registers the i18next instance used by useTranslation.
 *
 * @param instance The app's i18next instance.
 */
export function setI18n(instance: I18nInstance): void {
  const target = instance as I18nLike;
  current = target;
  if (attached.has(target)) return;
  attached.add(target);
  const notify = () => listeners.forEach((listener) => listener());
  target.on("languageChanged", notify);
  target.on("loaded", notify);
}

/**
 * Returns the registered i18next instance, or null before registration.
 */
export function getI18n(): I18nInstance | null {
  return current as I18nInstance | null;
}

/**
 * Returns a translator bound to the given namespace, re-rendering when the
 * language changes.
 *
 * @param ns Namespace to bind, or undefined for the default namespace.
 * @returns An object carrying the bound `t`.
 */
export function useTranslation(ns?: string): { t: Translator } {
  const [, setVersion] = useState(0);

  useEffect(() => {
    const bump = () => setVersion((version) => version + 1);
    listeners.add(bump);
    return () => {
      listeners.delete(bump);
    };
  }, []);

  const instance = current;
  const language = instance?.language;

  const t = useMemo<Translator>(() => {
    if (!instance) {
      return (key) => key;
    }
    return (key, options) => {
      const value = instance.t(key, {
        ...options,
        lng: language ?? undefined,
        ns,
      });
      return typeof value === "string" ? value : String(value);
    };
  }, [instance, language, ns]);

  return { t };
}
