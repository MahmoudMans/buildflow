"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { ApiClientError, apiGet } from "@/lib/api";
import type { ChangeOrderStatus } from "@/lib/change-order-types";
import {
  type ChangeOrderImpact,
  buildImpactRows,
  describeChangeOrderProfit,
  describeWarning,
  isPendingStatus,
} from "@/lib/change-order-impact";
import { formatMoneyExact } from "@/lib/format";

export function ChangeOrderImpactView({
  impact,
  currency,
}: {
  impact: ChangeOrderImpact;
  currency: string;
}) {
  const rows = buildImpactRows(impact, currency);

  return (
    <section className="uxImpact" aria-label="Impact of approval">
      <header className="uxImpactHeader">
        <h4>Impact of approval</h4>
        <p>
          What the project looks like if {impact.changeOrderNumber} is
          approved.
        </p>
      </header>

      {impact.warnings.length > 0 && (
        <ul className="uxImpactWarnings">
          {impact.warnings.map((code) => (
            <li key={code}>{describeWarning(code)}</li>
          ))}
        </ul>
      )}

      <div className="uxImpactTableWrap">
        <table className="uxImpactTable">
          <caption className="uxSrOnly">
            Project figures now and after approving this change order
          </caption>

          <thead>
            <tr>
              <th scope="col">Figure</th>
              <th scope="col">Now</th>
              <th scope="col">After approval</th>
              <th scope="col">Change</th>
            </tr>
          </thead>

          <tbody>
            {rows.map((row) => (
              <tr key={row.key}>
                <th scope="row">{row.label}</th>
                <td>{row.before}</td>
                <td>{row.after}</td>
                <td className="change" data-tone={row.tone}>
                  {row.change}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="uxImpactNote">
        This change order: {describeChangeOrderProfit(impact, currency)}.
      </p>

      {impact.pipeline.pendingCount > 1 && (
        <p className="uxImpactNote">
          {impact.pipeline.pendingCount} change orders on this project are
          still pending: together{" "}
          {formatMoneyExact(impact.pipeline.pendingCostImpact, currency)} of
          cost and{" "}
          {formatMoneyExact(
            impact.pipeline.pendingClientPriceImpact,
            currency,
          )}{" "}
          of client price.
        </p>
      )}

      <p className="uxImpactNote">
        The forecast cost assumes this change order&apos;s cost has not been
        committed or spent yet.
      </p>
    </section>
  );
}

export function ChangeOrderImpactPanel({
  changeOrderId,
  organizationId,
  currency,
  status,
  version,
}: {
  changeOrderId: string;
  organizationId: string;
  currency: string;
  status: ChangeOrderStatus;
  version?: string;
}) {
  const enabled = isPendingStatus(status);

  const [impact, setImpact] = useState<ChangeOrderImpact | null>(null);
  const [state, setState] = useState<"loading" | "ready" | "error">(
    "loading",
  );
  const [message, setMessage] = useState("");
  const requestSequence = useRef(0);

  const load = useCallback(async () => {
    const requestId = requestSequence.current + 1;

    requestSequence.current = requestId;

    setState("loading");

    try {
      const result = await apiGet<ChangeOrderImpact>(
        `/change-orders/${changeOrderId}/impact`,
        organizationId,
      );

      if (requestSequence.current !== requestId) {
        return;
      }

      setImpact(result);
      setState("ready");
    } catch (caught) {
      if (requestSequence.current !== requestId) {
        return;
      }

      setMessage(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load the impact preview.",
      );
      setState("error");
    }
  }, [changeOrderId, organizationId]);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    const timer = window.setTimeout(() => {
      void load();
    }, 0);

    return () => {
      window.clearTimeout(timer);
    };
  }, [enabled, load, status, version]);

  if (!enabled) {
    return null;
  }

  if (state === "error") {
    return (
      <section className="uxImpact" role="status">
        <header className="uxImpactHeader">
          <h4>Impact of approval</h4>
          <p>{message}</p>
        </header>

        <div>
          <button
            type="button"
            className="uxLinkButton"
            onClick={() => {
              void load();
            }}
          >
            Try again
          </button>
        </div>
      </section>
    );
  }

  // Never show the numbers of another change order while this one loads.
  const shown =
    impact && impact.changeOrderId === changeOrderId ? impact : null;

  if (!shown) {
    return (
      <section className="uxImpact" role="status" aria-live="polite">
        <header className="uxImpactHeader">
          <h4>Impact of approval</h4>
          <p>Calculating the effect on the project…</p>
        </header>
      </section>
    );
  }

  if (!shown.pending) {
    return null;
  }

  return <ChangeOrderImpactView impact={shown} currency={currency} />;
}
