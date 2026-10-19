/**
 * @fileoverview Framework-neutral server surface shared by the Fastify dev
 * adapter and the Bun.serve production server.
 */

/**
 * Node-compatible response surface. The SSR streamer writes into this and both
 * adapters map it onto a concrete response.
 */
export type NodeResponseLike = {
  /**
   * HTTP status code.
   */
  statusCode: number;
  /**
   * Sets a response header.
   */
  setHeader(name: string, value: string): void;
  /**
   * Reads a response header.
   */
  getHeader(name: string): string | undefined;
  /**
   * Removes a response header.
   */
  removeHeader(name: string): void;
  /**
   * Writes a chunk to the response body.
   */
  write(chunk: string | Uint8Array): unknown;
  /**
   * Ends the response, optionally writing a final chunk.
   */
  end(chunk?: string | Uint8Array): void;
};

/**
 * Request passed to route handlers.
 */
export type SunRequest = {
  /**
   * HTTP method, uppercase.
   */
  method: string;
  /**
   * Path plus query string.
   */
  url: string;
  /**
   * Node-shaped raw request, for handlers reading `raw.url`.
   */
  raw: { url: string; method: string };
  /**
   * Lowercased request headers.
   */
  headers: Record<string, string | undefined>;
  /**
   * Parsed request body, or undefined for bodyless requests.
   */
  body: unknown;
  /**
   * Parsed query string.
   */
  query: Record<string, string | string[]>;
  /**
   * Captured route parameters.
   */
  params: Record<string, string>;
  /**
   * Client IP address.
   */
  ip: string;
  /**
   * Request protocol, `http` or `https`.
   */
  protocol: string;
};

/**
 * Reply passed to route handlers.
 */
export type SunReply = {
  /**
   * Sets a response header.
   */
  header(name: string, value: string): SunReply;
  /**
   * Reads a response header.
   */
  getHeader(name: string): string | undefined;
  /**
   * Removes a response header.
   */
  removeHeader(name: string): void;
  /**
   * Sets the status code.
   */
  status(code: number): SunReply;
  /**
   * Sets the status code. Alias of status.
   */
  code(code: number): SunReply;
  /**
   * Sends a response body and ends the request.
   */
  send(body?: unknown): void;
  /**
   * Sends a redirect.
   */
  redirect(url: string, code?: number): SunReply;
  /**
   * Delegates to the registered not-found handler.
   */
  callNotFound(): void;
  /**
   * Node-compatible response used by the SSR streamer.
   */
  raw: NodeResponseLike;
};

/**
 * Async route handler.
 */
export type RouteHandler = (request: SunRequest, reply: SunReply) => void | Promise<void>;

/**
 * Lifecycle hook names supported by the adapters.
 */
export type HookName = "onRequest" | "preHandler" | "onSend";

/**
 * Lifecycle hook.
 */
export type Hook = (
  request: SunRequest,
  reply: SunReply,
  payload?: unknown,
) => void | Promise<void>;

/**
 * Server surface both adapters implement so app route code is unchanged.
 */
export interface SunApp {
  /**
   * Registers a GET route.
   */
  get(path: string, handler: RouteHandler): void;
  /**
   * Registers a POST route.
   */
  post(path: string, handler: RouteHandler): void;
  /**
   * Registers a route with an explicit method.
   */
  route(options: { method: string; url: string; handler: RouteHandler }): void;
  /**
   * Registers a plugin. Handled natively by the Bun server, forwarded to
   * Fastify in development.
   */
  register(plugin: unknown, options?: unknown): void | Promise<void>;
  /**
   * Registers a lifecycle hook.
   */
  addHook(name: HookName, hook: Hook): void;
  /**
   * Registers the GET not-found handler.
   */
  setNotFoundHandler(options: { method: string[] }, handler: RouteHandler): void;
}
