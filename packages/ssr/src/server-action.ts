import { executeMutation } from "./client-mutation";
import type { MutationResult } from "./client-mutation";
import type { TypedDocumentNode } from "./mutations";

/**
 * Schema describing a form's fields and their primitive kind.
 */
export type FormFieldSchema = Record<string, "string" | "number" | "boolean">;

/**
 * Built action returned by {@link defineAction}.
 */
export interface DefinedAction<
  TDoc extends TypedDocumentNode<unknown, unknown>,
> {
  /**
   * Invokes the action with typed variables.
   */
  run: (
    variables: TDoc extends TypedDocumentNode<unknown, infer V> ? V : never,
  ) => Promise<MutationResult>;
}

/**
 * Defines a typed client-side server action for a registered mutation path.
 */
export function defineAction<
  TDoc extends TypedDocumentNode<unknown, unknown>,
>(opts: {
  /**
   * Registered mutation path the action posts to.
   */
  path: string;
  /**
   * Generated document supplying the variable type.
   */
  document: TDoc;
  /**
   * Optional override for Redirect results; defaults to a full-page navigation.
   */
  onRedirect?: (redirectTo: string) => void;
}): DefinedAction<TDoc> {
  return {
    run: async (variables) => {
      const result = await executeMutation(
        opts.path,
        variables as Record<string, unknown>,
      );
      if (result.__typename === "Redirect") {
        const onRedirect = opts.onRedirect ?? defaultRedirect;
        onRedirect(result.redirectTo);
      }
      return result;
    },
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

function defaultRedirect(redirectTo: string): void {
  if (typeof window !== "undefined") {
    window.location.assign(redirectTo);
  }
}
