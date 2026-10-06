import { describe, expect, it } from "vitest";

import { formatDateTime, formatMoneyExact } from "./format";

describe("formatMoneyExact", () => {
  it("keeps the cents", () => {
    expect(formatMoneyExact(1234.5, "EUR")).toBe("€1,234.50");
  });

  it("uses the currency it is given", () => {
    expect(formatMoneyExact(99, "USD")).toBe("$99.00");
  });
});

describe("formatDateTime", () => {
  it("shows the date and the time", () => {
    const value = new Date(2026, 9, 6, 14, 5).toISOString();
    const text = formatDateTime(value);

    expect(text).toContain("06 Oct 2026");
    expect(text).toContain("14:05");
  });

  it("falls back for missing or invalid values", () => {
    expect(formatDateTime(null)).toBe("—");
    expect(formatDateTime(undefined)).toBe("—");
    expect(formatDateTime("not a date", "n/a")).toBe("n/a");
  });
});
