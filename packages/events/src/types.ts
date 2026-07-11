/**
 * An event map where K = event name, V = event payload type.
 * Apps define their own maps - the package never needs to know about them.
 */
export interface EventMap {
  [key: string]: unknown;
}

/** Extract a valid event key from an EventMap. */
export type EventKey<T extends EventMap> = string & keyof T;

/** Extract the payload type for a specific event key. */
export type EventPayload<T extends EventMap, K extends EventKey<T>> = T[K];

/** A handler function for a specific payload type. */
export type EventHandler<T> = (payload: T) => void;
