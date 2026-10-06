"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { ApiClientError, apiGet } from "@/lib/api";
import type {
  AttentionItem,
  AttentionKind,
  AttentionSeverity,
  AttentionSummary,
} from "@/lib/attention-types";
import { formatDate, formatMoney } from "@/lib/format";

const KIND_LABEL: Record<AttentionKind, string> = {
  PROJECT_AT_RISK: "Project at risk",
  OVERDUE_TASK: "Overdue task",
  OPEN_INCIDENT: "Open incident",
  OVERDUE_INVOICE: "Overdue invoice",
  CHANGE_ORDER_REVIEW: "Change order in client review",
};

const SEVERITY_LABEL: Record<AttentionSeverity, string> = {
  CRITICAL: "Critical",
  WARNING: "Warning",
  INFO: "Info",
};

const INITIAL_VISIBLE = 8;

type Chip = {
  kind: AttentionKind;
  label: string;
  count: number;
};

function itemHref(item: AttentionItem) {
  const base = `/app/projects/${item.projectId}`;

  switch (item.kind) {
    case "OVERDUE_TASK":
      return `${base}/tasks/${item.entityId}`;
    case "OPEN_INCIDENT":
      return `${base}/site`;
    case "OVERDUE_INVOICE":
      return `${base}/commercial`;
    case "CHANGE_ORDER_REVIEW":
      return `${base}/change-orders`;
    default:
      return base;
  }
}

export function AttentionPanel({
  organizationId,
  currency,
}: {
  organizationId: string;
  currency: string;
}) {
  const [summary, setSummary] = useState<AttentionSummary | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">(
    "loading",
  );
  const [message, setMessage] = useState("");
  const [filter, setFilter] = useState<AttentionKind | "ALL">("ALL");
  const [expanded, setExpanded] = useState(false);
  const requestSequence = useRef(0);

  const load = useCallback(async () => {
    const requestId = requestSequence.current + 1;

    requestSequence.current = requestId;

    setStatus("loading");

    try {
      const result = await apiGet<AttentionSummary>(
        "/dashboard/attention",
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
          : "Could not load the items that need attention.",
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

  const filtered = useMemo(() => {
    const items = summary?.items ?? [];

    return filter === "ALL"
      ? items
      : items.filter((item) => item.kind === filter);
  }, [summary, filter]);

  if (status === "error") {
    return (
      <section className="uxAttention" role="alert">
        <header className="uxAttentionHeader">
          <div>
            <h2>Needs attention</h2>
            <p>{message}</p>
          </div>

          <button
            type="button"
            className="uxLinkButton"
            onClick={() => {
              void load();
            }}
          >
            Try again
          </button>
        </header>
      </section>
    );
  }

  if (!summary) {
    return (
      <section className="uxAttention" role="status" aria-live="polite">
        <header className="uxAttentionHeader">
          <div>
            <h2>Needs attention</h2>
            <p>Checking your projects…</p>
          </div>
        </header>
      </section>
    );
  }

  const chips: Chip[] = [
    { kind: "PROJECT_AT_RISK", label: "At risk", count: summary.projectsAtRisk },
    {
      kind: "OVERDUE_TASK",
      label: "Overdue tasks",
      count: summary.overdueTasks,
    },
    {
      kind: "OPEN_INCIDENT",
      label: "Open incidents",
      count: summary.openIncidents,
    },
    ...(summary.financeVisible
      ? [
          {
            kind: "OVERDUE_INVOICE" as AttentionKind,
            label: "Overdue invoices",
            count: summary.overdueInvoices,
          },
        ]
      : []),
    {
      kind: "CHANGE_ORDER_REVIEW",
      label: "Awaiting client",
      count: summary.changeOrdersAwaitingClient,
    },
  ];

  const total = chips.reduce((sum, chip) => sum + chip.count, 0);

  const shown = expanded ? filtered : filtered.slice(0, INITIAL_VISIBLE);

  return (
    <section className="uxAttention" aria-labelledby="attention-heading">
      <header className="uxAttentionHeader">
        <div>
          <h2 id="attention-heading">Needs attention</h2>

          <p>
            {total === 0
              ? "Nothing needs attention in your projects right now."
              : `${total} ${total === 1 ? "item" : "items"} across your projects, most urgent first.`}
          </p>
        </div>

        <button
          type="button"
          className="uxLinkButton"
          onClick={() => {
            void load();
          }}
        >
          Refresh
        </button>
      </header>

      {total > 0 && (
        <div
          className="uxAttentionChips"
          role="group"
          aria-label="Filter by category"
        >
          {chips.map((chip) => (
            <button
              type="button"
              key={chip.kind}
              className="uxAttentionChip"
              aria-pressed={filter === chip.kind}
              disabled={chip.count === 0}
              onClick={() => {
                setFilter((current) =>
                  current === chip.kind ? "ALL" : chip.kind,
                );
                setExpanded(false);
              }}
            >
              <span>{chip.label}</span>
              <strong>{chip.count}</strong>
            </button>
          ))}
        </div>
      )}

      {shown.length > 0 && (
        <ul className="uxAttentionList">
          {shown.map((item) => (
            <li
              className="uxAttentionItem"
              key={`${item.kind}-${item.entityId}`}
            >
              <Link href={itemHref(item)}>
                <span
                  className={`uxSeverityDot ${item.severity.toLowerCase()}`}
                  aria-hidden="true"
                />

                <span className="uxAttentionBody">
                  <span className="uxAttentionKind">
                    {KIND_LABEL[item.kind]} · {SEVERITY_LABEL[item.severity]}
                  </span>

                  <strong>{item.title}</strong>

                  <span className="uxAttentionDetail">
                    {item.projectName} ({item.projectNumber}) · {item.detail}
                  </span>
                </span>

                <span className="uxAttentionMeta">
                  {item.amount !== null && (
                    <strong>{formatMoney(item.amount, currency)}</strong>
                  )}

                  {item.date && (
                    <time dateTime={item.date}>{formatDate(item.date)}</time>
                  )}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {filtered.length > INITIAL_VISIBLE && (
        <button
          type="button"
          className="uxLinkButton"
          onClick={() => setExpanded((value) => !value)}
        >
          {expanded ? "Show fewer" : `Show all ${filtered.length}`}
        </button>
      )}
    </section>
  );
}
