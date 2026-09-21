/**
 * Thrown when a mutation fails. Carries the HTTP status when one is known.
 */
export class MutationError extends Error {
  readonly statusCode?: number;

  constructor(message: string, statusCode?: number) {
    super(message);
    this.name = "MutationError";
    this.statusCode = statusCode;
  }
}
