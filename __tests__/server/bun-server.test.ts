/**
 * Tests for the Bun production server response state.
 */

import { createResponseState } from "@ssr/bun-server";

describe("createResponseState", () => {
  it("stops writing once the client cancels the body stream", async () => {
    const state = createResponseState();
    state.sink.write("first");
    const response = state.seal();
    const reader = response.body!.getReader();

    await reader.read();
    await reader.cancel();

    expect(() => state.sink.write("second")).not.toThrow();
    expect(() => state.sink.end("last")).not.toThrow();
  });

  it("ignores writes after the response has ended", () => {
    const state = createResponseState();
    state.sink.write("first");
    state.sink.end();

    expect(() => state.sink.write("second")).not.toThrow();
    expect(() => state.sink.end("last")).not.toThrow();
  });
});
