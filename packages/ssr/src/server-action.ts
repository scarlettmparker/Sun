import { executeMutation } from "./client-mutation";
import type { VariablesOf } from "./mutations";

/**
 * Schema describing a form's fields and their primitive kind.
 */
export type FormFieldSchema = Record<string, "string" | "number" | "boolean">;

/**
 * Built action returned by {@link defineAction}.
 */
export interface DefinedAction<TDoc, TResponse = unknown> {
  /**
   * Invokes the action with typed variables.
   */
  run: (variables: VariablesOf<TDoc>) => Promise<TResponse>;
}

/**
 * Defines a typed client-side server action for a registered mutation path.
 */
export function defineAction<TDoc, TResponse = unknown>(opts: {
  /**
   * Registered mutation path the action posts to.
   */
  path: string;
  /**
   * Generated document supplying the variable type.
   */
  document: TDoc;
}): DefinedAction<TDoc, TResponse> {
  return {
    run: (variables) =>
      executeMutation<TResponse>(
        opts.path,
        variables as Record<string, unknown>,
      ),
  };
}

type FormKindMap = {
  string: string;
  number: number;
  boolean: boolean;
};

/**
 * Resolved field types for a parsed form, keyed by the schema.
 */
export type FormValues<T extends FormFieldSchema> = {
  [K in keyof T]: FormKindMap[T[K]];
};

/**
 * Parses form entries into typed primitives, centralising the form boundary.
 */
export function parseForm<T extends FormFieldSchema>(
  values: { get: (name: string) => string | null },
  schema: T,
): FormValues<T> {
  const out = {} as FormValues<T>;
  for (const [key, kind] of Object.entries(schema) as [keyof T, T[keyof T]][]) {
    const raw = values.get(String(key));
    if (kind === "number") {
      out[key] = Number(raw) as never;
    } else if (kind === "boolean") {
      out[key] = (raw === "true") as never;
    } else {
      out[key] = (raw ?? "") as never;
    }
  }
  return out;
}
