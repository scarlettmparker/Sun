import { MutationResult } from "~/server/actions/utils";

export class ServerRedirectError extends Error {
  public statusCode: number;
  public redirectTo: string;
  public cacheInvalidateKey?: string;
  public clientPayload?: MutationResult;

  constructor(
    redirectTo: string,
    cacheInvalidateKey?: string,
    clientPayload?: MutationResult,
    statusCode: number = 302
  ) {
    super(`Redirecting to ${redirectTo}`);
    this.name = "ServerRedirectError";
    this.redirectTo = redirectTo;
    this.cacheInvalidateKey = cacheInvalidateKey;
    this.clientPayload = clientPayload;
    this.statusCode = statusCode;
  }
}
