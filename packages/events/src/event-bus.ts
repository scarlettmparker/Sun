import type { EventMap, EventKey, EventPayload, EventHandler } from "./types";

/**
 * Typed in-process event bus. Apps create an instance with their own EventMap
 * to get full type safety on emit/on/off calls.
 */
export class EventBus<T extends EventMap> {
  private listeners = new Map<string, Set<EventHandler<any>>>();

  /**
   * Subscribe to a typed event.
   * Returns an unsubscribe function.
   */
  on<K extends EventKey<T>>(
    event: K,
    handler: EventHandler<EventPayload<T, K>>,
  ): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(handler);
    return () => this.off(event, handler);
  }

  /**
   * Unsubscribe a specific handler from an event.
   */
  off<K extends EventKey<T>>(
    event: K,
    handler: EventHandler<EventPayload<T, K>>,
  ): void {
    this.listeners.get(event)?.delete(handler);
  }

  /**
   * Emit a typed event to all local subscribers.
   */
  emit<K extends EventKey<T>>(event: K, payload: EventPayload<T, K>): void {
    this.listeners.get(event)?.forEach((handler) => handler(payload));
  }

  /**
   * Remove all listeners. Useful for cleanup.
   */
  clear(): void {
    this.listeners.clear();
  }
}
