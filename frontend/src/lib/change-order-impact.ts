import type { ChangeOrderStatus } from "./change-order-types";
import { formatDate, formatMoneyExact } from "./format";
import { humanizeStatus } from "./ux-validation";

export interface ImpactPosition {
  budget: number;
  contractValue: number;
  plannedProfit: number;
  plannedMarginPercentage: number;
  forecastCost: number;
  forecastProfit: number;
  forecastMarginPercentage: number;
  budgetHeadroom: number;
  plannedEndDate: string | null;
}

export interface ImpactPipeline {
  pendingCount: number;
  pendingCostImpact: number;
  pendingClientPriceImpact: number;
}

export interface ChangeOrderImpact {
  changeOrderId: string;
  projectId: string;
  changeOrderNumber: string;
  title: string;
  status: ChangeOrderStatus;
  pending: boolean;
  alreadyApproved: boolean;
  costImpact: number;
  clientPriceImpact: number;
  scheduleImpactDays: number;
  changeOrderProfit: number;
  changeOrderMarginPercentage: number | null;
  current: ImpactPosition;
  afterApproval: ImpactPosition;
  warnings: string[];
  pipeline: ImpactPipeline;
}

export type ImpactTone = "good" | "bad" | "neutral";

export interface ImpactRow {
  key: string;
  label: string;
  before: string;
  after: string;
  change: string;
  tone: ImpactTone;
}

const MINUS = "−";
const NO_CHANGE = "—";

// Amounts and percentages closer to zero than this count as "no change".
const EPSILON = 0.005;

const WARNING_TEXT: Record<string, string> = {
  CHANGE_ORDER_LOSES_MONEY:
    "The client price is below the internal cost, so this change order loses money.",
  FORECAST_OVER_BUDGET: "The forecast cost would exceed the budget.",
  FORECAST_LOSS: "The project forecast would turn into a loss.",
  MARGIN_DROPS:
    "The planned margin would fall by more than one percentage point.",
  SCHEDULE_DELAY: "The planned end date would move later.",
};

export function isPendingStatus(status: ChangeOrderStatus): boolean {
  return (
    status === "DRAFT" ||
    status === "SUBMITTED" ||
    status === "CLIENT_REVIEW"
  );
}

export function describeWarning(code: string): string {
  return WARNING_TEXT[code] ?? humanizeStatus(code);
}

export function signedMoney(delta: number, currency: string): string {
  if (Math.abs(delta) < EPSILON) {
    return NO_CHANGE;
  }

  return `${delta < 0 ? MINUS : "+"}${formatMoneyExact(
    Math.abs(delta),
    currency,
  )}`;
}

export function signedPoints(delta: number): string {
  if (Math.abs(delta) < EPSILON) {
    return NO_CHANGE;
  }

  return `${delta < 0 ? MINUS : "+"}${Math.abs(delta).toFixed(2)} pts`;
}

export function signedDays(days: number): string {
  if (days === 0 || Number.isNaN(days)) {
    return NO_CHANGE;
  }

  const count = Math.abs(days);

  return `${days < 0 ? MINUS : "+"}${count} ${count === 1 ? "day" : "days"}`;
}

export function formatPercent(value: number): string {
  return `${value < 0 ? MINUS : ""}${Math.abs(value).toFixed(1)}%`;
}

function dayNumber(value: string | null): number | null {
  if (!value) {
    return null;
  }

  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);

  if (!match) {
    return null;
  }

  return Math.round(
    Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])) /
      86_400_000,
  );
}

function toneFor(delta: number, higherIsBetter: boolean): ImpactTone {
  if (Math.abs(delta) < EPSILON) {
    return "neutral";
  }

  return delta > 0 === higherIsBetter ? "good" : "bad";
}

export function buildImpactRows(
  impact: ChangeOrderImpact,
  currency: string,
): ImpactRow[] {
  const now = impact.current;
  const then = impact.afterApproval;

  const money = (
    key: string,
    label: string,
    before: number,
    after: number,
    higherIsBetter: boolean | null,
  ): ImpactRow => ({
    key,
    label,
    before: formatMoneyExact(before, currency),
    after: formatMoneyExact(after, currency),
    change: signedMoney(after - before, currency),
    tone:
      higherIsBetter === null
        ? "neutral"
        : toneFor(after - before, higherIsBetter),
  });

  const endBefore = dayNumber(now.plannedEndDate);
  const endAfter = dayNumber(then.plannedEndDate);

  const endDelta =
    endBefore === null || endAfter === null ? null : endAfter - endBefore;

  return [
    money("budget", "Budget", now.budget, then.budget, null),
    money(
      "contractValue",
      "Contract value",
      now.contractValue,
      then.contractValue,
      null,
    ),
    money(
      "plannedProfit",
      "Planned profit",
      now.plannedProfit,
      then.plannedProfit,
      true,
    ),
    {
      key: "plannedMargin",
      label: "Planned margin",
      before: formatPercent(now.plannedMarginPercentage),
      after: formatPercent(then.plannedMarginPercentage),
      change: signedPoints(
        then.plannedMarginPercentage - now.plannedMarginPercentage,
      ),
      tone: toneFor(
        then.plannedMarginPercentage - now.plannedMarginPercentage,
        true,
      ),
    },
    money(
      "forecastCost",
      "Forecast cost",
      now.forecastCost,
      then.forecastCost,
      null,
    ),
    money(
      "forecastProfit",
      "Forecast profit",
      now.forecastProfit,
      then.forecastProfit,
      true,
    ),
    money(
      "budgetHeadroom",
      "Budget headroom",
      now.budgetHeadroom,
      then.budgetHeadroom,
      true,
    ),
    {
      key: "plannedEndDate",
      label: "Planned end date",
      before: formatDate(now.plannedEndDate, NO_CHANGE),
      after: formatDate(then.plannedEndDate, NO_CHANGE),
      change: endDelta === null ? NO_CHANGE : signedDays(endDelta),
      tone: endDelta === null ? "neutral" : toneFor(endDelta, false),
    },
  ];
}

export function describeChangeOrderProfit(
  impact: ChangeOrderImpact,
  currency: string,
): string {
  const profit = impact.changeOrderProfit;

  if (Math.abs(profit) < EPSILON) {
    return "Break-even";
  }

  const margin =
    impact.changeOrderMarginPercentage === null
      ? ""
      : ` (${formatPercent(impact.changeOrderMarginPercentage)} margin)`;

  return `${profit > 0 ? "Earns" : "Loses"} ${formatMoneyExact(
    Math.abs(profit),
    currency,
  )}${margin}`;
}
