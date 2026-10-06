"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";

import { ApiClientError, apiGet } from "@/lib/api";
import { formatMoney } from "@/lib/format";
import type {
  AgingBucketKey,
  ReceivablesSummary,
} from "@/lib/receivables-types";

const BUCKET_CLASS: Record<AgingBucketKey, string> = {
  CURRENT: "current",
  DAYS_1_30: "d30",
  DAYS_31_60: "d60",
  DAYS_61_90: "d90",
  DAYS_90_PLUS: "d90plus",
};

const COMPACT = new Intl.NumberFormat("en-GB", {
  notation: "compact",
  maximumFractionDigits: 1,
});

function monthLabel(month: string) {
  const date = new Date(`${month}-01T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return month;
  }

  return new Intl.DateTimeFormat("en-GB", { month: "short" }).format(date);
}

function plural(count: number, noun: string) {
  return `${count} ${noun}${count === 1 ? "" : "s"}`;
}

export function ReceivablesPanel({
  organizationId,
  currency,
}: {
  organizationId: string;
  currency: string;
}) {
  const [summary, setSummary] = useState<ReceivablesSummary | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">(
    "loading",
  );
  const [message, setMessage] = useState("");
  const requestSequence = useRef(0);

  const load = useCallback(async () => {
    const requestId = requestSequence.current + 1;

    requestSequence.current = requestId;

    setStatus("loading");

    try {
      const result = await apiGet<ReceivablesSummary>(
        "/dashboard/receivables",
        organizationId,
      );

      if (requestSequence.current !== requestId) {
        return;
      }

      setSummary(result);
      setStatus("ready");
    } catch (caught) {
      if (requestSequence.current !== requestId) {
        return;
      }

      setMessage(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load receivables.",
      );
      setStatus("error");
    }
  }, [organizationId]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);

    return () => {
      window.clearTimeout(timer);
    };
  }, [load]);

  if (status === "error") {
    return (
      <section className="uxReceivables" role="alert">
        <header className="uxReceivablesHeader">
          <h2>Receivables</h2>
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

  if (!summary) {
    return (
      <section className="uxReceivables" role="status" aria-live="polite">
        <header className="uxReceivablesHeader">
          <h2>Receivables</h2>
          <p>Loading unpaid invoices…</p>
        </header>
      </section>
    );
  }

  if (!summary.available) {
    return null;
  }

  const notYetDue = Math.max(
    0,
    summary.totalOutstanding - summary.totalOverdue,
  );

  const maxExpected = Math.max(
    0,
    ...summary.expected.map((item) => item.amount),
  );

  return (
    <section className="uxReceivables" aria-labelledby="receivables-heading">
      <header className="uxReceivablesHeader">
        <h2 id="receivables-heading">Receivables</h2>

        <p>Unpaid invoices by age, and when the money is due to arrive.</p>
      </header>

      {summary.openInvoices === 0 ? (
        <p className="uxReceivablesNote">
          No unpaid invoices. Everything that has been issued is paid.
        </p>
      ) : (
        <>
          <dl className="uxReceivablesStats">
            <div>
              <dt>Outstanding</dt>
              <dd>{formatMoney(summary.totalOutstanding, currency)}</dd>
              <span>{plural(summary.openInvoices, "open invoice")}</span>
            </div>

            <div className={summary.totalOverdue > 0 ? "alert" : undefined}>
              <dt>Overdue</dt>
              <dd>{formatMoney(summary.totalOverdue, currency)}</dd>
              <span>
                {plural(summary.overdueInvoices, "invoice")} ·{" "}
                {summary.overduePercentage}% of outstanding
              </span>
            </div>

            <div>
              <dt>Not yet due</dt>
              <dd>{formatMoney(notYetDue, currency)}</dd>
              <span>
                {plural(
                  summary.openInvoices - summary.overdueInvoices,
                  "invoice",
                )}
              </span>
            </div>
          </dl>

          <div className="uxReceivablesGrid">
            <div className="uxReceivablesBlock">
              <h3>Age of unpaid invoices</h3>

              <div
                className="uxAgingBar"
                role="img"
                aria-label={`Unpaid invoices by age: ${summary.aging
                  .map(
                    (bucket) =>
                      `${bucket.label} ${formatMoney(bucket.amount, currency)}`,
                  )
                  .join(", ")}`}
              >
                {summary.aging.map((bucket) =>
                  bucket.amount > 0 ? (
                    <span
                      key={bucket.key}
                      className={`uxAgingSegment ${BUCKET_CLASS[bucket.key]}`}
                      style={{
                        width: `${
                          (bucket.amount / summary.totalOutstanding) * 100
                        }%`,
                      }}
                    />
                  ) : null,
                )}
              </div>

              <ul className="uxAgingLegend">
                {summary.aging.map((bucket) => (
                  <li key={bucket.key}>
                    <span
                      className={`uxAgingSwatch ${BUCKET_CLASS[bucket.key]}`}
                      aria-hidden="true"
                    />

                    <span>
                      {bucket.label} <small>({bucket.invoices})</small>
                    </span>

                    <strong>{formatMoney(bucket.amount, currency)}</strong>
                  </li>
                ))}
              </ul>
            </div>

            <div className="uxReceivablesBlock">
              <h3>Expected collections</h3>

              <div
                className="uxExpected"
                role="img"
                aria-label={`Expected collections by month: ${summary.expected
                  .map(
                    (item) =>
                      `${monthLabel(item.month)} ${formatMoney(item.amount, currency)}`,
                  )
                  .join(", ")}`}
              >
                {summary.expected.map((item) => (
                  <div className="uxExpectedCol" key={item.month}>
                    <span className="uxExpectedValue">
                      {item.amount > 0 ? COMPACT.format(item.amount) : "—"}
                    </span>

                    <span className="uxExpectedBarTrack">
                      <span
                        className="uxExpectedBar"
                        style={{
                          height:
                            item.amount > 0 && maxExpected > 0
                              ? `${Math.max(
                                  4,
                                  (item.amount / maxExpected) * 100,
                                )}%`
                              : "0%",
                        }}
                      />
                    </span>

                    <span className="uxExpectedMonth">
                      {monthLabel(item.month)}
                    </span>
                  </div>
                ))}
              </div>

              <p className="uxReceivablesNote">
                Not yet due, grouped by invoice due date.
                {summary.laterAmount > 0
                  ? ` A further ${formatMoney(summary.laterAmount, currency)} is due after these six months.`
                  : ""}
              </p>
            </div>
          </div>

          {summary.projects.length > 0 && (
            <div className="uxReceivablesBlock">
              <h3>Projects with money outstanding</h3>

              <div className="uxReceivablesTableWrap">
                <table className="uxReceivablesTable">
                  <thead>
                    <tr>
                      <th scope="col">Project</th>
                      <th scope="col" className="num">
                        Outstanding
                      </th>
                      <th scope="col" className="num">
                        Overdue
                      </th>
                      <th scope="col" className="num">
                        Oldest overdue
                      </th>
                    </tr>
                  </thead>

                  <tbody>
                    {summary.projects.map((project) => (
                      <tr key={project.projectId}>
                        <td>
                          <Link
                            href={`/app/projects/${project.projectId}/commercial`}
                          >
                            {project.projectName}
                          </Link>

                          <small>{project.projectNumber}</small>
                        </td>

                        <td className="num">
                          {formatMoney(project.outstanding, currency)}
                        </td>

                        <td
                          className={
                            project.overdue > 0 ? "num alert" : "num"
                          }
                        >
                          {formatMoney(project.overdue, currency)}
                        </td>

                        <td className="num">
                          {project.oldestOverdueDays > 0
                            ? plural(project.oldestOverdueDays, "day")
                            : "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  );
}
