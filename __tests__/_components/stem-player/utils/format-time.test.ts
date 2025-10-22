/**
 * @brief
 * Tests for formatTime utility function.
 * Tests the formatting of time from seconds to M:SS format, proper padding and conversion.
 */

import { formatTime } from "~/_components/stem-player/utils/format-time";

describe("formatTime", () => {
  it("formats seconds to M:SS with zero-padded seconds", () => {
    expect(formatTime(0)).toBe("0:00");
    expect(formatTime(59)).toBe("0:59");
    expect(formatTime(60)).toBe("1:00");
    expect(formatTime(61)).toBe("1:01");
  });

  it("handles minutes correctly", () => {
    expect(formatTime(120)).toBe("2:00");
    expect(formatTime(3599)).toBe("59:59");
    expect(formatTime(3600)).toBe("60:00");
  });

  it("handles decimal seconds by flooring them", () => {
    expect(formatTime(30.7)).toBe("0:30");
    expect(formatTime(90.9)).toBe("1:30");
    expect(formatTime(61.5)).toBe("1:01");
  });

  it("handles large values", () => {
    expect(formatTime(86400)).toBe("1440:00"); // 24 hours
    expect(formatTime(90061)).toBe("1501:01"); // 25 hours + 1 minute + 1 second
  });

  it("handles edge cases", () => {
    expect(formatTime(0)).toBe("0:00");
    expect(formatTime(1)).toBe("0:01");
    expect(formatTime(59)).toBe("0:59");
    expect(formatTime(60)).toBe("1:00");
  });

  it("ensures seconds are always two digits", () => {
    expect(formatTime(0)).toBe("0:00");
    expect(formatTime(1)).toBe("0:01");
    expect(formatTime(10)).toBe("0:10");
    expect(formatTime(59)).toBe("0:59");
    expect(formatTime(60)).toBe("1:00");
    expect(formatTime(61)).toBe("1:01");
  });

  it("handles negative values", () => {
    // flooring negative numbers
    expect(formatTime(-1)).toBe("-0:01");
    expect(formatTime(-60)).toBe("-1:00");
  });
});
