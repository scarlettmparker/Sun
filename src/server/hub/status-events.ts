import { EventEmitter } from "node:events";

const RESCAN_EVENT = "rescan";

const emitter = new EventEmitter();

/**
 * Subscribes to status rescan requests.
 */
export function onStatusRescan(listener: () => void): void {
  emitter.on(RESCAN_EVENT, listener);
}

/**
 * Unsubscribes a status rescan listener.
 */
export function offStatusRescan(listener: () => void): void {
  emitter.off(RESCAN_EVENT, listener);
}

/**
 * Requests connected status streams to re-probe immediately.
 */
export function emitStatusRescan(): void {
  emitter.emit(RESCAN_EVENT);
}
