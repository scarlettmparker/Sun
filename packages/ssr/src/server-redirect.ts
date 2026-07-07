import type { MutationResult } from "./client-mutation";

export class ServerRedirectError extends Error {
  public statusCode: number;
  public redirectTo: string;
  public cacheInvalidateKey?: string | string[];
  public clientPayload?: MutationResult;
  public cookies?: string[];

  constructor(
    redirectTo: string,
    cacheInvalidateKey?: string | string[],
    clientPayload?: MutationResult,
    statusCode: number = 302,
    cookies?: string[],
  ) {
    super(`Redirecting to ${redirectTo}`);
    this.name = "ServerRedirectError";
    this.redirectTo = redirectTo;
    this.cacheInvalidateKey = cacheInvalidateKey;
    this.clientPayload = clientPayload;
    this.statusCode = statusCode;
    this.cookies = cookies;
  }
}
