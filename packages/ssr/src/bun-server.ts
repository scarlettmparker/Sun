/**
 * @fileoverview Bun.serve production server implementing the neutral server
 * surface. Development still runs on Fastify through the adapter.
 */

import path from "node:path";
import { withCspNonce } from "./csp-nonce";
import { runWithRequestContext } from "./request-context";
import type {
  Hook,
  HookName,
  NodeResponseLike,
  RouteHandler,
  SunApp,
  SunReply,
  SunRequest,
} from "./server-types";

type Route = {
  /**
   * Uppercase HTTP method.
   */
  method: string;
  /**
   * Path segments, including named params and a trailing wildcard.
   */
  segments: string[];
  /**
   * Handler invoked on a match.
   */
  handler: RouteHandler;
};

type BunRequestIP = {
  /**
   * Peer address, when Bun can resolve it.
   */
  address?: string;
} | null;

type BunServerHandle = {
  /**
   * Stops the server.
   */
  stop(closeActiveConnections?: boolean): void | Promise<void>;
  /**
   * Bound port.
   */
  port: number;
  /**
   * Bound hostname.
   */
  hostname: string;
  /**
   * Resolves a request's peer address.
   */
  requestIP(request: Request): BunRequestIP;
};

type BunServeOptions = {
  /**
   * Port to bind.
   */
  port: number;
  /**
   * Host to bind.
   */
  hostname?: string;
  /**
   * Request handler.
   */
  fetch(request: Request, server: BunServerHandle): Response | Promise<Response>;
};

type BunFile = Blob & {
  /**
   * Checks whether the file exists without reading it.
   */
  exists(): Promise<boolean>;
};

type BunLike = {
  /**
   * Starts an HTTP server.
   */
  serve(options: BunServeOptions): BunServerHandle;
  /**
   * Reads a file lazily.
   */
  file(path: string): BunFile;
};

/**
 * Response state accumulated by a handler and turned into a Response on seal.
 */
type ResponseState = {
  /**
   * Node-compatible sink the SSR streamer writes into.
   */
  sink: NodeResponseLike;
  /**
   * Response headers.
   */
  headers: Headers;
  /**
   * Sets the status code.
   */
  setStatus(code: number): void;
  /**
   * Sets a buffered body.
   */
  setBody(body: string | Uint8Array): void;
  /**
   * Builds the final Response.
   */
  seal(): Response;
};

/**
 * Options for {@link startBunServer}.
 */
export type BunServerOptions = {
  /**
   * Port to bind.
   */
  port: number;
  /**
   * Host to bind.
   */
  host: string;
  /**
   * Registers the shared app routes and hooks.
   */
  register: (app: SunApp) => void | Promise<void>;
};

/**
 * Handle returned once the server is listening.
 */
export type BunServerHandleInfo = {
  /**
   * Bound address.
   */
  address: string;
  /**
   * Stops the server.
   */
  stop: () => void | Promise<void>;
};

/**
 * Reads the Bun runtime from the global scope.
 *
 * @returns The runtime surface used by this module.
 */
function bunRuntime(): BunLike {
  const runtime = (globalThis as unknown as { Bun?: BunLike }).Bun;
  if (!runtime) {
    throw new Error("Bun.serve server requires the Bun runtime");
  }
  return runtime;
}

/**
 * Splits a pathname into non-empty segments.
 *
 * @param pathname The request path.
 * @returns The path segments.
 */
function splitPath(pathname: string): string[] {
  return pathname.split("/").filter(Boolean);
}

/**
 * Matches a route against path segments.
 *
 * @param route The registered route.
 * @param parts The request path segments.
 * @returns Captured params, or null when the route does not match.
 */
function matchRoute(route: Route, parts: string[]): Record<string, string> | null {
  const params: Record<string, string> = {};
  for (let i = 0; i < route.segments.length; i += 1) {
    const segment = route.segments[i];
    if (segment === "*") return params;
    const actual = parts[i];
    if (actual === undefined) return null;
    if (segment.startsWith(":")) {
      params[segment.slice(1)] = actual;
    } else if (segment !== actual) {
      return null;
    }
  }
  return parts.length === route.segments.length ? params : null;
}

/**
 * Copies request headers into a lowercased record.
 *
 * @param headers The request headers.
 * @returns The lowercased header record.
 */
function headersToRecord(headers: Headers): Record<string, string> {
  const out: Record<string, string> = {};
  headers.forEach((value, key) => {
    out[key.toLowerCase()] = value;
  });
  return out;
}

/**
 * Parses a query string, keeping repeated keys as arrays.
 *
 * @param search The query string.
 * @returns The parsed query.
 */
function parseQuery(search: string): Record<string, string | string[]> {
  const params = new URLSearchParams(search);
  const out: Record<string, string | string[]> = {};
  for (const key of new Set(params.keys())) {
    const values = params.getAll(key);
    out[key] = values.length > 1 ? values : values[0];
  }
  return out;
}

/**
 * Parses a request body by content type.
 *
 * @param request The incoming request.
 * @returns The parsed body, or undefined when there is none.
 */
async function parseBody(request: Request): Promise<unknown> {
  const method = request.method.toUpperCase();
  if (method === "GET" || method === "HEAD") return undefined;
  const contentType = request.headers.get("content-type") ?? "";
  const text = await request.text();
  if (!text) return undefined;
  if (contentType.includes("application/json")) {
    try {
      return JSON.parse(text);
    } catch {
      return undefined;
    }
  }
  if (contentType.includes("application/x-www-form-urlencoded")) {
    const out: Record<string, string> = {};
    for (const [key, value] of new URLSearchParams(text)) out[key] = value;
    return out;
  }
  return text;
}

/**
 * Creates the per-request response state.
 *
 * @returns The response state.
 */
export function createResponseState(): ResponseState {
  const encoder = new TextEncoder();
  const headers = new Headers();
  let statusCode = 200;
  let body: string | Uint8Array | null = null;
  let stream: ReadableStream<Uint8Array> | null = null;
  let controller: ReadableStreamDefaultController<Uint8Array> | null = null;
  let ended = false;
  let cancelled = false;

  /**
   * Lazily creates the body stream.
   *
   * @returns The stream controller.
   */
  const ensureStream = (): ReadableStreamDefaultController<Uint8Array> => {
    if (!controller) {
      stream = new ReadableStream<Uint8Array>({
        start(c) {
          controller = c;
        },
        cancel() {
          cancelled = true;
        },
      });
    }
    return controller!;
  };

  const sink: NodeResponseLike = {
    get statusCode() {
      return statusCode;
    },
    set statusCode(value: number) {
      statusCode = value;
    },
    setHeader(name, value) {
      headers.set(name, value);
    },
    getHeader(name) {
      return headers.get(name) ?? undefined;
    },
    removeHeader(name) {
      headers.delete(name);
    },
    write(chunk) {
      if (ended || cancelled) return true;
      try {
        ensureStream().enqueue(typeof chunk === "string" ? encoder.encode(chunk) : chunk);
      } catch {
        cancelled = true;
      }
      return true;
    },
    end(chunk) {
      if (ended || cancelled) return;
      if (chunk !== undefined) sink.write(chunk);
      ended = true;
      try {
        controller?.close();
      } catch {
        cancelled = true;
      }
    },
  };

  return {
    sink,
    headers,
    setStatus(code) {
      statusCode = code;
    },
    setBody(value) {
      body = value;
    },
    seal() {
      if (body !== null) {
        return new Response(body, { status: statusCode, headers });
      }
      return new Response(stream, { status: statusCode, headers });
    },
  };
}

/**
 * Creates the neutral reply backed by a response state.
 *
 * @param state The per-request response state.
 * @returns The reply.
 */
function createReply(state: ResponseState): SunReply & { settled: boolean } {
  const reply = {
    settled: false,
    header(name: string, value: string) {
      state.headers.set(name, value);
      return reply;
    },
    getHeader(name: string) {
      return state.headers.get(name) ?? undefined;
    },
    removeHeader(name: string) {
      state.headers.delete(name);
    },
    status(code: number) {
      state.setStatus(code);
      return reply;
    },
    code(code: number) {
      state.setStatus(code);
      return reply;
    },
    send(body?: unknown) {
      if (body === undefined || body === null) {
        state.setBody("");
      } else if (typeof body === "string") {
        if (!state.headers.has("content-type")) {
          state.headers.set("content-type", "text/plain; charset=utf-8");
        }
        state.setBody(body);
      } else if (body instanceof Uint8Array) {
        if (!state.headers.has("content-type")) {
          state.headers.set("content-type", "application/octet-stream");
        }
        state.setBody(body);
      } else {
        if (!state.headers.has("content-type")) {
          state.headers.set("content-type", "application/json; charset=utf-8");
        }
        state.setBody(JSON.stringify(body));
      }
      reply.settled = true;
    },
    redirect(url: string, code?: number) {
      state.headers.set("Location", url);
      state.setStatus(code ?? 302);
      state.setBody("");
      reply.settled = true;
      return reply;
    },
    callNotFound() {
      state.setStatus(404);
      state.setBody("Not Found");
      reply.settled = true;
    },
    raw: state.sink,
  };
  return reply;
}

/**
 * Starts the Bun.serve production server.
 *
 * @param options Bind address and the shared route registration callback.
 * @returns The bound address and a stop function.
 */
export async function startBunServer(options: BunServerOptions): Promise<BunServerHandleInfo> {
  const runtime = bunRuntime();
  const routes: Route[] = [];
  const hooks: Record<HookName, Hook[]> = {
    onRequest: [],
    preHandler: [],
    onSend: [],
  };
  let notFound: RouteHandler | null = null;

  /**
   * Registers a route.
   *
   * @param method The HTTP method.
   * @param url The route path.
   * @param handler The route handler.
   */
  const add = (method: string, url: string, handler: RouteHandler): void => {
    routes.push({
      method: method.toUpperCase(),
      segments: splitPath(url),
      handler,
    });
  };

  const app: SunApp = {
    get(url, handler) {
      add("GET", url, handler);
    },
    post(url, handler) {
      add("POST", url, handler);
    },
    route(spec) {
      add(spec.method, spec.url, spec.handler);
    },
    register() {
      return undefined;
    },
    addHook(name, hook) {
      hooks[name].push(hook);
    },
    setNotFoundHandler(_spec, handler) {
      notFound = handler;
    },
  };

  await options.register(app);

  const clientRoot = path.resolve("dist/client");
  const messagesRoot = path.resolve("messages");

  /**
   * Serves a static file when the path maps to one.
   *
   * @param pathname The request path.
   * @returns The file response, or null when there is no match.
   */
  const serveStatic = async (pathname: string): Promise<Response | null> => {
    const rel = decodeURIComponent(pathname);
    let base: string;
    let abs: string;
    let cache: string;
    if (rel.startsWith("/messages/")) {
      base = messagesRoot;
      abs = path.join(messagesRoot, rel.slice("/messages/".length));
      cache = "public, max-age=3600";
    } else if (rel.startsWith("/assets/") || rel.startsWith("/fonts/")) {
      base = clientRoot;
      abs = path.join(clientRoot, rel.slice(1));
      cache = "public, max-age=31536000, immutable";
    } else if (/\.[a-zA-Z0-9]+$/.test(rel)) {
      base = clientRoot;
      abs = path.join(clientRoot, rel.slice(1));
      cache = "public, max-age=86400";
    } else {
      return null;
    }
    if (!abs.startsWith(base + path.sep)) return null;
    const file = runtime.file(abs);
    if (!(await file.exists())) return null;
    return new Response(file, {
      headers: {
        "Cache-Control": cache,
        "X-Content-Type-Options": "nosniff",
      },
    });
  };

  /**
   * Handles one HTTP request.
   *
   * @param request The incoming request.
   * @param server The Bun server handle.
   * @returns The final response.
   */
  const handle = async (request: Request, server: BunServerHandle): Promise<Response> => {
    const url = new URL(request.url);
    const method = request.method.toUpperCase();
    const headers = headersToRecord(request.headers);
    const ip = server.requestIP(request)?.address ?? "";
    const forwardedProto = headers["x-forwarded-proto"];
    const protocol = forwardedProto === "https" || url.protocol === "https:" ? "https" : "http";

    if (method === "GET" || method === "HEAD") {
      const staticResponse = await serveStatic(url.pathname);
      if (staticResponse) return staticResponse;
    }

    const body = await parseBody(request);
    const query = parseQuery(url.search);
    const parts = splitPath(url.pathname);

    let matched: { route: Route; params: Record<string, string> } | null = null;
    for (const route of routes) {
      if (route.method !== method) continue;
      const params = matchRoute(route, parts);
      if (params) {
        matched = { route, params };
        break;
      }
    }

    return withCspNonce(() =>
      runWithRequestContext({ cookie: headers["cookie"], ip }, async () => {
        const state = createResponseState();
        const reply = createReply(state);
        const sunRequest: SunRequest = {
          method,
          url: url.pathname + url.search,
          raw: { url: url.pathname + url.search, method },
          headers,
          body,
          query,
          params: matched?.params ?? {},
          ip,
          protocol,
        };

        for (const hook of hooks.onRequest) {
          await hook(sunRequest, reply);
          if (reply.settled) break;
        }

        if (!reply.settled) {
          for (const hook of hooks.preHandler) {
            await hook(sunRequest, reply);
            if (reply.settled) break;
          }
        }

        if (!reply.settled) {
          if (matched) {
            await matched.route.handler(sunRequest, reply);
          } else if (notFound) {
            await notFound(sunRequest, reply);
          } else {
            state.setStatus(404);
            state.setBody("Not Found");
          }
        }

        for (const hook of hooks.onSend) await hook(sunRequest, reply);
        return state.seal();
      }),
    );
  };

  const server = runtime.serve({
    port: options.port,
    hostname: options.host,
    fetch: handle,
  });

  return {
    address: `${server.hostname}:${server.port}`,
    stop: () => server.stop(),
  };
}
