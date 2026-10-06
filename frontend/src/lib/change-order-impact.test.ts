import { describe, expect, it } from "vitest";

import {
  type ChangeOrderImpact,
  buildImpactRows,
  describeChangeOrderProfit,
  describeWarning,
  formatPercent,
  isPendingStatus,
  signedDays,
  signedMoney,
  signedPoints,
} from "./change-order-impact";

const MINUS = "−";
const DASH = "—";

function impact(overrides: Partial<ChangeOrderImpact> = {}): ChangeOrderImpact {
  return {
    changeOrderId: "co-1",
    projectId: "project-1",
    changeOrderNumber: "CO-2026-0001",
    title: "Extra waterproofing",
    status: "SUBMITTED",
    pending: true,
    alreadyApproved: false,
    costImpact: 10000,
    clientPriceImpact: 15000,
    scheduleImpactDays: 14,
    changeOrderProfit: 5000,
    changeOrderMarginPercentage: 33.3333,
    current: {
      budget: 85000,
      contractValue: 108000,
      plannedProfit: 23000,
      plannedMarginPercentage: 21.2963,
      forecastCost: 0,
      forecastProfit: 108000,
      forecastMarginPercentage: 100,
      budgetHeadroom: 85000,
      plannedEndDate: "2027-06-30",
    },
    afterApproval: {
      budget: 95000,
      contractValue: 123000,
      plannedProfit: 28000,
      plannedMarginPercentage: 22.7642,
      forecastCost: 10000,
      forecastProfit: 113000,
      forecastMarginPercentage: 91.8699,
      budgetHeadroom: 85000,
      plannedEndDate: "2027-07-14",
    },
    warnings: ["SCHEDULE_DELAY"],
    pipeline: {
      pendingCount: 2,
      pendingCostImpact: 13000,
      pendingClientPriceImpact: 17000,
    },
    ...overrides,
  };
}

describe("signed values", () => {
  it("shows money with an explicit sign", () => {
    expect(signedMoney(5000, "EUR")).toBe("+€5,000.00");
    expect(signedMoney(-1000.5, "EUR")).toBe(`${MINUS}€1,000.50`);
    expect(signedMoney(0, "EUR")).toBe(DASH);
    expect(signedMoney(0.001, "EUR")).toBe(DASH);
  });

  it("shows percentage points, days and percentages", () => {
    expect(signedPoints(1.4679)).toBe("+1.47 pts");
    expect(signedPoints(-2)).toBe(`${MINUS}2.00 pts`);
    expect(signedPoints(0)).toBe(DASH);

    expect(signedDays(14)).toBe("+14 days");
    expect(signedDays(1)).toBe("+1 day");
    expect(signedDays(-3)).toBe(`${MINUS}3 days`);
    expect(signedDays(0)).toBe(DASH);

    expect(formatPercent(21.2963)).toBe("21.3%");
    expect(formatPercent(-50)).toBe(`${MINUS}50.0%`);
  });
});

describe("isPendingStatus", () => {
  it("is true only while the change order can still be approved", () => {
    expect(isPendingStatus("DRAFT")).toBe(true);
    expect(isPendingStatus("SUBMITTED")).toBe(true);
    expect(isPendingStatus("CLIENT_REVIEW")).toBe(true);
    expect(isPendingStatus("APPROVED")).toBe(false);
    expect(isPendingStatus("REJECTED")).toBe(false);
    expect(isPendingStatus("CANCELLED")).toBe(false);
  });
});

describe("describeWarning", () => {
  it("has plain wording for every known warning", () => {
    for (const code of [
      "CHANGE_ORDER_LOSES_MONEY",
      "FORECAST_OVER_BUDGET",
      "FORECAST_LOSS",
      "MARGIN_DROPS",
      "SCHEDULE_DELAY",
    ]) {
      expect(describeWarning(code)).toMatch(/\s/);
      expect(describeWarning(code)).not.toContain("_");
    }
  });

  it("makes an unknown code readable instead of hiding it", () => {
    expect(describeWarning("SOMETHING_NEW")).toBe("Something New");
  });
});

describe("buildImpactRows", () => {
  it("compares the project now and after approval", () => {
    const rows = buildImpactRows(impact(), "EUR");
    const byKey = Object.fromEntries(rows.map((row) => [row.key, row]));

    expect(rows.map((row) => row.key)).toEqual([
      "budget",
      "contractValue",
      "plannedProfit",
      "plannedMargin",
      "forecastCost",
      "forecastProfit",
      "budgetHeadroom",
      "plannedEndDate",
    ]);

    expect(byKey.budget).toMatchObject({
      before: "€85,000.00",
      after: "€95,000.00",
      change: "+€10,000.00",
      tone: "neutral",
    });

    expect(byKey.plannedProfit).toMatchObject({
      before: "€23,000.00",
      after: "€28,000.00",
      change: "+€5,000.00",
      tone: "good",
    });

    expect(byKey.plannedMargin).toMatchObject({
      before: "21.3%",
      after: "22.8%",
      change: "+1.47 pts",
      tone: "good",
    });

    expect(byKey.forecastCost).toMatchObject({
      before: "€0.00",
      after: "€10,000.00",
      tone: "neutral",
    });

    expect(byKey.budgetHeadroom).toMatchObject({
      change: DASH,
      tone: "neutral",
    });

    expect(byKey.plannedEndDate).toMatchObject({
      before: "30 Jun 2027",
      after: "14 Jul 2027",
      change: "+14 days",
      tone: "bad",
    });
  });

  it("marks a falling profit and margin as bad", () => {
    const base = impact();

    const rows = buildImpactRows(
      impact({
        afterApproval: {
          ...base.afterApproval,
          plannedProfit: 20000,
          plannedMarginPercentage: 18,
        },
      }),
      "EUR",
    );

    expect(rows.find((row) => row.key === "plannedProfit")?.tone).toBe(
      "bad",
    );
    expect(rows.find((row) => row.key === "plannedMargin")?.tone).toBe(
      "bad",
    );
  });

  it("copes with a project that has no planned end date", () => {
    const base = impact();

    const rows = buildImpactRows(
      impact({
        current: { ...base.current, plannedEndDate: null },
        afterApproval: { ...base.afterApproval, plannedEndDate: null },
      }),
      "EUR",
    );

    expect(rows.find((row) => row.key === "plannedEndDate")).toMatchObject({
      before: DASH,
      after: DASH,
      change: DASH,
      tone: "neutral",
    });
  });
});

describe("describeChangeOrderProfit", () => {
  it("says whether the change order earns or loses money", () => {
    expect(describeChangeOrderProfit(impact(), "EUR")).toBe(
      "Earns €5,000.00 (33.3% margin)",
    );

    expect(
      describeChangeOrderProfit(
        impact({
          changeOrderProfit: -1000,
          changeOrderMarginPercentage: -50,
        }),
        "EUR",
      ),
    ).toBe(`Loses €1,000.00 (${MINUS}50.0% margin)`);

    expect(
      describeChangeOrderProfit(
        impact({ changeOrderProfit: 0, changeOrderMarginPercentage: 0 }),
        "EUR",
      ),
    ).toBe("Break-even");

    expect(
      describeChangeOrderProfit(
        impact({
          changeOrderProfit: 2000,
          changeOrderMarginPercentage: null,
        }),
        "EUR",
      ),
    ).toBe("Earns €2,000.00");
  });
});
