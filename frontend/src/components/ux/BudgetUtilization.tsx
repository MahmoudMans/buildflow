import { formatMoney } from "@/lib/format";

function BudgetBar({
  label,
  amount,
  budget,
  scaleMax,
  currency,
}: {
  label: string;
  amount: number;
  budget: number;
  scaleMax: number;
  currency: string;
}) {
  const safeAmount = Math.max(0, amount);
  const over = safeAmount > budget;
  const usedPercent = Math.round((safeAmount / budget) * 100);
  const withinWidth = (Math.min(safeAmount, budget) / scaleMax) * 100;
  const overWidth = over ? ((safeAmount - budget) / scaleMax) * 100 : 0;
  const markerLeft = (budget / scaleMax) * 100;

  const summary = over
    ? `${formatMoney(safeAmount - budget, currency)} over budget (${usedPercent}% of revised budget)`
    : `${usedPercent}% of revised budget`;

  return (
    <div className="uxBudgetRow">
      <div className="uxBudgetHeader">
        <span>{label}</span>
        <strong>{formatMoney(safeAmount, currency)}</strong>
      </div>

      <div
        className="uxBudgetTrack"
        role="img"
        aria-label={`${label}: ${formatMoney(safeAmount, currency)}. ${summary}.`}
      >
        <div className="uxBudgetFill" style={{ width: `${withinWidth}%` }} />

        {over && (
          <div
            className="uxBudgetOver"
            style={{ left: `${markerLeft}%`, width: `${overWidth}%` }}
          />
        )}

        <span
          className="uxBudgetMarker"
          style={{ left: `${markerLeft}%` }}
          aria-hidden="true"
        />
      </div>

      <p className={over ? "uxBudgetNote over" : "uxBudgetNote"}>{summary}</p>
    </div>
  );
}

export function BudgetUtilization({
  revisedBudget,
  actualCost,
  forecastCost,
  currency,
}: {
  revisedBudget: number;
  actualCost: number;
  forecastCost: number;
  currency: string;
}) {
  if (revisedBudget <= 0) {
    return (
      <div className="uxBudgetBars">
        <p className="uxBudgetNote">
          No revised budget set yet. Actual {formatMoney(actualCost, currency)},
          forecast {formatMoney(forecastCost, currency)}.
        </p>
      </div>
    );
  }

  const scaleMax = Math.max(revisedBudget, actualCost, forecastCost);

  return (
    <div className="uxBudgetBars">
      <BudgetBar
        label="Actual"
        amount={actualCost}
        budget={revisedBudget}
        scaleMax={scaleMax}
        currency={currency}
      />

      <BudgetBar
        label="Forecast"
        amount={forecastCost}
        budget={revisedBudget}
        scaleMax={scaleMax}
        currency={currency}
      />

      <p className="uxBudgetNote">
        <span className="uxBudgetKey" aria-hidden="true" />
        Revised budget: {formatMoney(revisedBudget, currency)}
      </p>
    </div>
  );
}
