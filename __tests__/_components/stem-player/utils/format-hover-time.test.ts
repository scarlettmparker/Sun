/**
 * @brief
 * Tests for formatHoverTime utility function.
 * Tests the formatting of time values for hover display, correct precision based on time ranges.
 */
import { formatHoverTime } from "~/_components/stem-player/utils/format-hover-time";

describe("formatHoverTime", () => {
  it("formats seconds for values less than 1 minute", () => {
    expect(formatHoverTime(0)).toBe("0s");
    expect(formatHoverTime(30)).toBe("30s");
    expect(formatHoverTime(59)).toBe("59s");
  });

  it("formats minutes and seconds for values less than 1 hour", () => {
    expect(formatHoverTime(60)).toBe("1m 0s");
    expect(formatHoverTime(90)).toBe("1m 30s");
    expect(formatHoverTime(3599)).toBe("59m 59s");
  });

  it("formats hours, minutes, and seconds for values 1 hour or more", () => {
    expect(formatHoverTime(3600)).toBe("1h 0m 0s");
    expect(formatHoverTime(3661)).toBe("1h 1m 1s");
    expect(formatHoverTime(7265)).toBe("2h 1m 5s");
  });

  it("handles decimal seconds by flooring them", () => {
    expect(formatHoverTime(30.7)).toBe("30s");
    expect(formatHoverTime(90.9)).toBe("1m 30s");
    expect(formatHoverTime(3661.5)).toBe("1h 1m 1s");
  });

  it("handles edge cases around boundaries", () => {
    expect(formatHoverTime(59.9)).toBe("59s");
    expect(formatHoverTime(60)).toBe("1m 0s");
    expect(formatHoverTime(3599.9)).toBe("59m 59s");
    expect(formatHoverTime(3600)).toBe("1h 0m 0s");
  });

  it("handles large values correctly", () => {
    expect(formatHoverTime(86400)).toBe("24h 0m 0s"); // 1 day
    expect(formatHoverTime(90061)).toBe("25h 1m 1s"); // 1 day + 1 hour + 1 minute + 1 second
  });

  it("handles zero and negative values", () => {
    expect(formatHoverTime(0)).toBe("0s");
    // though negative might not be expected, it should handle gracefully
    expect(formatHoverTime(-1)).toBe("-1s");
  });
});
