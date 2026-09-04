import { afterEach, describe, expect, it, vi } from "vitest";
import {
  formatDate,
  formatDateTime,
  formatDeadline,
  formatExperience,
  formatRelativeDays,
  formatScore,
  orUnknown,
} from "../app/lib/format";

describe("formatDate", () => {
  it("renders an instant as a UTC day", () => {
    expect(formatDate("2026-09-03T11:22:33Z")).toBe("2026-09-03");
  });

  it("says unknown for absent and unparseable values", () => {
    expect(formatDate(null)).toBe("unknown");
    expect(formatDate(undefined)).toBe("unknown");
    expect(formatDate("")).toBe("unknown");
    expect(formatDate("not a date")).toBe("unknown");
  });
});

describe("formatDateTime", () => {
  it("renders to the minute in UTC", () => {
    expect(formatDateTime("2026-09-03T11:22:33Z")).toBe("2026-09-03 11:22");
  });

  it("says unknown rather than Invalid Date", () => {
    expect(formatDateTime("nonsense")).toBe("unknown");
  });
});

describe("formatRelativeDays", () => {
  afterEach(() => vi.useRealTimers());

  it("names today, yesterday, the past and the future", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-09-10T12:00:00Z"));
    expect(formatRelativeDays("2026-09-10T12:00:00Z")).toBe("today");
    expect(formatRelativeDays("2026-09-09T12:00:00Z")).toBe("1 day ago");
    expect(formatRelativeDays("2026-09-03T12:00:00Z")).toBe("7 days ago");
    expect(formatRelativeDays("2026-09-13T12:00:00Z")).toBe("in 3 days");
  });
});

describe("formatScore", () => {
  it("rounds to a whole score and keeps zero", () => {
    expect(formatScore(72.4)).toBe("72");
    expect(formatScore(72.6)).toBe("73");
    // zero is a real score, not a missing one
    expect(formatScore(0)).toBe("0");
  });

  it("says unknown when a job has not been scored", () => {
    expect(formatScore(null)).toBe("unknown");
    expect(formatScore(undefined)).toBe("unknown");
  });
});

describe("formatExperience", () => {
  it("covers each shape a requirement can take", () => {
    expect(formatExperience(3, 5)).toBe("3-5 yrs");
    expect(formatExperience(3, null)).toBe("3+ yrs");
    expect(formatExperience(null, 5)).toBe("up to 5 yrs");
    // a zero-year minimum is the junior signal the whole console is built around
    expect(formatExperience(0, null)).toBe("entry");
    expect(formatExperience(null, null)).toBe("unknown");
  });
});

describe("formatDeadline", () => {
  it("prefers the open-ended flag over any date", () => {
    expect(formatDeadline("2026-10-01T00:00:00Z", true)).toBe("continuous");
    expect(formatDeadline(null, true)).toBe("continuous");
  });

  it("falls back to the date, then to unknown", () => {
    expect(formatDeadline("2026-10-01T00:00:00Z", false)).toBe("2026-10-01");
    expect(formatDeadline(null, false)).toBe("unknown");
  });
});

describe("orUnknown", () => {
  it("treats empty string as missing but keeps other falsy-looking text", () => {
    expect(orUnknown("")).toBe("unknown");
    expect(orUnknown(null)).toBe("unknown");
    expect(orUnknown(undefined)).toBe("unknown");
    expect(orUnknown("0")).toBe("0");
  });
});
