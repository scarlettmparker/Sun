/**
 * @fileoverview Adapts a Fastify instance to the framework-neutral {@link SunApp}
 * surface, so app route code runs unchanged in development.
 */

import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import type { HTTPMethods } from "fastify";
import type { HookName, RouteHandler, SunApp, SunReply, SunRequest } from "./server-types";

/**
 * Maps a Fastify request onto the neutral shape.
 */
function toSunRequest(request: FastifyRequest): SunRequest {
  return {
    method: request.method,
    url: request.url,
    raw: { url: request.raw.url ?? request.url, method: request.method },
    headers: request.headers as unknown as Record<string, string | undefined>,
    body: request.body,
    query: request.query as Record<string, string | string[]>,
    params: (request.params ?? {}) as Record<string, string>,
    ip: request.ip,
    protocol: request.protocol,
  };
}

/**
 * Maps a Fastify reply onto the neutral shape.
 */
function toSunReply(reply: FastifyReply): SunReply {
  const sun: SunReply = {
    header(name, value) {
      reply.header(name, value);
      return sun;
    },
    getHeader(name) {
      const value = reply.getHeader(name);
      if (value === undefined) return undefined;
      return typeof value === "string" ? value : String(value);
    },
    removeHeader(name) {
      reply.removeHeader(name);
    },
    status(code) {
      reply.code(code);
      return sun;
    },
    code(code) {
      reply.code(code);
      return sun;
    },
    send(body) {
      reply.send(body);
    },
    redirect(url, code) {
      reply.redirect(url, code ?? 302);
      return sun;
    },
    callNotFound() {
      reply.callNotFound();
    },
    raw: reply.raw,
  };
  return sun;
}

/**
 * Wraps a Fastify instance so it can be driven through the neutral surface.
 *
 * @param app The Fastify instance.
 * @returns The adapter.
 */
export function fastifyApp(app: FastifyInstance): SunApp {
  /**
   * Wraps a neutral handler as a Fastify handler.
   *
   * @param handler The neutral handler.
   * @returns The Fastify handler.
   */
  const wrap = (handler: RouteHandler) => (request: FastifyRequest, reply: FastifyReply) =>
    handler(toSunRequest(request), toSunReply(reply));

  return {
    get(path, handler) {
      app.get(path, wrap(handler));
    },
    post(path, handler) {
      app.post(path, wrap(handler));
    },
    route(options) {
      app.route({
        method: options.method as HTTPMethods,
        url: options.url,
        handler: wrap(options.handler),
      });
    },
    async register(plugin, options) {
      await app.register(
        plugin as Parameters<FastifyInstance["register"]>[0],
        options as Parameters<FastifyInstance["register"]>[1],
      );
    },
    addHook(name: HookName, hook) {
      if (name === "onSend") {
        app.addHook("onSend", async (request, reply, payload) => {
          await hook(toSunRequest(request), toSunReply(reply), payload);
          return payload;
        });
      } else if (name === "onRequest") {
        app.addHook("onRequest", async (request, reply) => {
          await hook(toSunRequest(request), toSunReply(reply));
        });
      } else {
        app.addHook("preHandler", async (request, reply) => {
          await hook(toSunRequest(request), toSunReply(reply));
        });
      }
    },
    setNotFoundHandler(options, handler) {
      app.setNotFoundHandler({ method: options.method as HTTPMethods[] }, wrap(handler));
    },
  };
}
