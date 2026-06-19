import type { EventMap, EventKey, EventPayload } from "./types";
import { EventBus } from "./event-bus";

const BRIDGE_CHANNEL = "@sun/events";

export interface BridgeOptions {
  /**
   * The target window (parent or iframe contentWindow).
   */
  target: Window;
  /**
   * Origin to validate against. Use "*" to allow any origin.
   */
  origin: string;
  /**
   * Channel name to namespace messages.
   */
  channel?: string;
}

/**
 * Bridges two EventBuses across an iframe boundary via postMessage.
 *
 * - `localBus`: events this side sends (emit goes to both local listeners AND postMessage).
 * - `remoteBus`: events arriving from the other side (populated from incoming postMessage).
 *
 * Usage:
 *   const localBus  = new EventBus<MyEvents>();
 *   const remoteBus = new EventBus<TheirEvents>();
 *   const bridge = new PostMessageBridge(localBus, remoteBus, {
 *     target: iframe.contentWindow!,
 *     origin: "https://other-app.example.com",
 *   });
 *
 *   // Send to the other side:
 *   bridge.send("my-event", { foo: "bar" });
 *
 *   // Receive from the other side:
 *   remoteBus.on("their-event", (payload) => { ... });
 *
 *   // Cleanup:
 *   bridge.destroy();
 */
export class PostMessageBridge<T extends EventMap> {
  private localBus: EventBus<T>;
  private remoteBus: EventBus<T>;
  private target: Window;
  private origin: string;
  private channel: string;
  private handleMessage: (event: MessageEvent) => void;

  constructor(
    localBus: EventBus<T>,
    remoteBus: EventBus<T>,
    options: BridgeOptions,
  ) {
    this.localBus = localBus;
    this.remoteBus = remoteBus;
    this.target = options.target;
    this.origin = options.origin;
    this.channel = options.channel ?? BRIDGE_CHANNEL;

    this.handleMessage = (event: MessageEvent) => {
      // Origin validation
      if (this.origin !== "*" && event.origin !== this.origin) return;
      if (event.source !== this.target) return;

      const data = event.data;
      if (!data || typeof data !== "object") return;
      if (data.__channel !== this.channel) return;
      if (typeof data.type !== "string") return;
      if (!("payload" in data)) return;

      // Forward to remote bus (typed)
      this.remoteBus.emit(data.type as EventKey<T>, data.payload);
    };

    window.addEventListener("message", this.handleMessage);
  }

  /**
   * Send an event across the iframe boundary.
   * Also emits locally so in-process subscribers receive it too.
   */
  send<K extends EventKey<T>>(
    event: K,
    payload: EventPayload<T, K>,
  ): void {
    this.localBus.emit(event, payload);
    this.target.postMessage(
      { __channel: this.channel, type: event, payload },
      this.origin,
    );
  }

  /**
   * Disconnect the bridge and clean up listeners.
   */
  destroy(): void {
    window.removeEventListener("message", this.handleMessage);
  }
}