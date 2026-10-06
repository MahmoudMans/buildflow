"use client";

import Link from "next/link";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  NotificationBell,
} from "@/components/NotificationBell";

import {
  AttentionPanel,
} from "@/components/AttentionPanel";

import {
  ReceivablesPanel,
} from "@/components/ReceivablesPanel";

import {
  useAuth,
} from "@/components/AuthProvider";

import {
  useOrganizationSelection,
} from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
} from "@/lib/api";

import type {
  DashboardMonthlyValue,
  OrganizationDashboard,
} from "@/lib/dashboard-types";

import styles from "./dashboard.module.css";

function money(
  value: number,
  currency: string,
) {
  return new Intl.NumberFormat(
    "en-GB",
    {
      style:
        "currency",
      currency,
      maximumFractionDigits:
        0,
    },
  ).format(
    value,
  );
}

function compactMoney(
  value: number,
  currency: string,
) {
  return new Intl.NumberFormat(
    "en-GB",
    {
      style:
        "currency",
      currency,
      notation:
        "compact",
      maximumFractionDigits:
        1,
    },
  ).format(
    value,
  );
}

function monthDate(
  value: string,
) {
  const [
    year,
    month,
  ] = value.split(
    "-",
  );

  return new Date(
    Number(
      year,
    ),
    Number(
      month,
    ) - 1,
    1,
  );
}

function monthLabel(
  value: string,
) {
  return new Intl.DateTimeFormat(
    "en-GB",
    {
      month:
        "short",
      year:
        "2-digit",
    },
  ).format(
    monthDate(
      value,
    ),
  );
}

function monthPeriod(
  values:
    DashboardMonthlyValue[],
) {
  if (
    values.length ===
    0
  ) {
    return "Period unavailable";
  }

  const first =
    monthDate(
      values[0]
        .month,
    );

  const last =
    monthDate(
      values[
        values.length - 1
      ].month,
    );

  const formatter =
    new Intl.DateTimeFormat(
      "en-GB",
      {
        month:
          "short",
        year:
          "numeric",
      },
    );

  if (
    first.getTime()
    === last.getTime()
  ) {
    return formatter.format(
      first,
    );
  }

  return `${formatter.format(
    first,
  )} – ${formatter.format(
    last,
  )}`;
}

function formatDeadline(
  value:
    string | null,
) {
  if (
    !value
  ) {
    return "Date unavailable";
  }

  return new Intl.DateTimeFormat(
    "en-GB",
    {
      day:
        "2-digit",
      month:
        "short",
      year:
        "numeric",
    },
  ).format(
    new Date(
      `${value}T00:00:00`,
    ),
  );
}

function formatRetrieved(
  value: Date,
) {
  return new Intl.DateTimeFormat(
    "en-GB",
    {
      day:
        "2-digit",
      month:
        "short",
      year:
        "numeric",
      hour:
        "2-digit",
      minute:
        "2-digit",
    },
  ).format(
    value,
  );
}

function humanize(
  value: string,
) {
  return value
    .toLowerCase()
    .split(
      "_",
    )
    .map(
      (part) =>
        part.length >
          0
          ? `${
              part[0]
                .toUpperCase()
            }${part.slice(
              1,
            )}`
          : part,
    )
    .join(
      " ",
    );
}

function MetricCard({
  label,
  value,
  qualifier,
}: {
  label: string;
  value:
    string | number;
  qualifier: string;
}) {
  return (
    <article
      className={
        styles.metricCard
      }
    >
      <span
        className={
          styles.metricLabel
        }
      >
        {label}
      </span>

      <strong
        className={
          styles.metricValue
        }
      >
        {value}
      </strong>

      <span
        className={
          styles.metricQualifier
        }
      >
        {qualifier}
      </span>
    </article>
  );
}

function MonthlyChart({
  title,
  values,
  currency,
  emptyMessage,
  tone,
}: {
  title: string;
  values:
    DashboardMonthlyValue[];
  currency: string;
  emptyMessage: string;
  tone:
    "revenue"
    | "expenses";
}) {
  const maxAbsolute =
    Math.max(
      0,
      ...values.map(
        (item) =>
          Math.abs(
            item.value,
          ),
      ),
    );

  const allZero =
    values.length >
      0
    && values.every(
      (item) =>
        item.value ===
        0,
    );

  const hasNegative =
    values.some(
      (item) =>
        item.value <
        0,
    );

  const period =
    monthPeriod(
      values,
    );

  return (
    <article
      className={
        styles.panel
      }
    >
      <header
        className={
          styles.panelHeader
        }
      >
        <div>
          <h3>
            {title}
          </h3>

          <p>
            {
              period
            }
          </p>
        </div>

        <span
          className={
            styles.unitBadge
          }
        >
          {currency}
        </span>
      </header>

      {
        values.length ===
          0
          ? (
              <div
                className={
                  styles.compactEmptyState
                }
              >
                <strong>
                  No records
                </strong>

                <span>
                  {
                    emptyMessage
                  }
                </span>
              </div>
            )
          : allZero
            ? (
                <>
                  <div
                    className={
                      styles.zeroSeries
                    }
                  >
                    <strong>
                      0
                    </strong>

                    <span>
                      All recorded monthly values are zero for this period.
                    </span>
                  </div>

                  <ChartDataDisclosure
                    values={
                      values
                    }
                    currency={
                      currency
                    }
                  />
                </>
              )
            : (
                <>
                  <div
                    className={
                      styles.chartArea
                    }
                    role="img"
                    aria-label={`${title}, ${period}, displayed in ${currency}. A complete numerical table follows the chart.`}
                  >
                    <div
                      className={
                        styles.chartScale
                      }
                      aria-hidden="true"
                    >
                      <span>
                        {
                          compactMoney(
                            maxAbsolute,
                            currency,
                          )
                        }
                      </span>

                      {
                        hasNegative
                          ? (
                              <span>
                                0
                              </span>
                            )
                          : null
                      }

                      <span>
                        {
                          hasNegative
                            ? compactMoney(
                                -maxAbsolute,
                                currency,
                              )
                            : "0"
                        }
                      </span>
                    </div>

                    <div
                      className={`${styles.chartColumns} ${
                        hasNegative
                          ? styles.signedChart
                          : ""
                      }`}
                    >
                      {
                        values.map(
                          (
                            item,
                          ) => {
                            const ratio =
                              maxAbsolute >
                                0
                                ? Math.abs(
                                    item.value,
                                  )
                                  / maxAbsolute
                                : 0;

                            const height =
                              hasNegative
                                ? ratio
                                  * 48
                                : ratio
                                  * 92;

                            const barClass =
                              tone ===
                                "revenue"
                                ? styles.revenueBar
                                : styles.expenseBar;

                            return (
                              <div
                                className={
                                  styles.chartColumn
                                }
                                key={
                                  item.month
                                }
                                title={`${monthLabel(
                                  item.month,
                                )}: ${money(
                                  item.value,
                                  currency,
                                )}`}
                              >
                                <div
                                  className={
                                    styles.barTrack
                                  }
                                >
                                  <span
                                    className={
                                      hasNegative
                                        ? styles.centerBaseline
                                        : styles.bottomBaseline
                                    }
                                    aria-hidden="true"
                                  />

                                  {
                                    item.value !==
                                      0
                                      ? (
                                          <span
                                            className={`${styles.chartBar} ${barClass}`}
                                            style={
                                              item.value >
                                              0
                                                ? {
                                                    height:
                                                      `${height}%`,
                                                    bottom:
                                                      hasNegative
                                                        ? "50%"
                                                        : "0",
                                                  }
                                                : {
                                                    height:
                                                      `${height}%`,
                                                    top:
                                                      "50%",
                                                  }
                                            }
                                            aria-hidden="true"
                                          />
                                        )
                                      : null
                                  }
                                </div>

                                <span
                                  className={
                                    styles.chartMonth
                                  }
                                >
                                  {
                                    monthLabel(
                                      item.month,
                                    )
                                  }
                                </span>
                              </div>
                            );
                          },
                        )
                      }
                    </div>
                  </div>

                  <ChartDataDisclosure
                    values={
                      values
                    }
                    currency={
                      currency
                    }
                  />
                </>
              )
      }
    </article>
  );
}

function ChartDataDisclosure({
  values,
  currency,
}: {
  values:
    DashboardMonthlyValue[];
  currency: string;
}) {
  return (
    <details
      className={
        styles.dataDisclosure
      }
    >
      <summary>
        View data
      </summary>

      <div
        className={
          styles.tableScroller
        }
      >
        <table
          className={
            styles.dataTable
          }
        >
          <thead>
            <tr>
              <th scope="col">
                Period
              </th>

              <th
                scope="col"
                className={
                  styles.numeric
                }
              >
                Value
              </th>
            </tr>
          </thead>

          <tbody>
            {
              values.map(
                (
                  item,
                ) => (
                  <tr
                    key={
                      item.month
                    }
                  >
                    <td>
                      {
                        monthLabel(
                          item.month,
                        )
                      }
                    </td>

                    <td
                      className={
                        styles.numeric
                      }
                    >
                      {
                        money(
                          item.value,
                          currency,
                        )
                      }
                    </td>
                  </tr>
                ),
              )
            }
          </tbody>
        </table>
      </div>
    </details>
  );
}

export default function DashboardPage() {
  const {
    user,
    organizations,
    loading:
      authLoading,
  } = useAuth();

  const selection =
    useOrganizationSelection(
      organizations,
    );

  const organizationId =
    selection.organizationId;

  const [
    dashboard,
    setDashboard,
  ] = useState<
    OrganizationDashboard
    | null
  >(
    null,
  );

  const [
    dashboardLoading,
    setDashboardLoading,
  ] = useState(
    true,
  );

  const [
    error,
    setError,
  ] = useState(
    "",
  );

  const [
    retrievedAt,
    setRetrievedAt,
  ] = useState<
    Date | null
  >(
    null,
  );

  const requestSequence =
    useRef(
      0,
    );

  const currentOrganization =
    useMemo(
      () =>
        organizations.find(
          (
            organization,
          ) =>
            organization.id
            === organizationId,
        ),
      [
        organizations,
        organizationId,
      ],
    );

  const currentRole =
    String(
      currentOrganization
        ?.role
      ?? "",
    );

  const canOpenReports =
    [
      "OWNER",
      "ADMIN",
      "ACCOUNTANT",
    ].includes(
      currentRole,
    );

  const load =
    useCallback(
      async () => {
        if (
          !organizationId
        ) {
          setDashboard(
            null,
          );

          setDashboardLoading(
            false,
          );

          return;
        }

        const requestId =
          requestSequence
            .current
          + 1;

        requestSequence
          .current =
            requestId;

        setError(
          "",
        );

        setDashboard(
          null,
        );

        setRetrievedAt(
          null,
        );

        setDashboardLoading(
          true,
        );

        try {
          const result =
            await apiGet<
              OrganizationDashboard
            >(
              "/dashboard",
              organizationId,
            );

          if (
            requestSequence
              .current
            !== requestId
          ) {
            return;
          }

          setDashboard(
            result,
          );

          setRetrievedAt(
            new Date(),
          );
        } catch (
          caught
        ) {
          if (
            requestSequence
              .current
            !== requestId
          ) {
            return;
          }

          setError(
            caught instanceof
              ApiClientError
              ? caught.message
              : caught instanceof
                    Error
                ? caught.message
                : "Unable to load dashboard.",
          );
        } finally {
          if (
            requestSequence
              .current
            === requestId
          ) {
            setDashboardLoading(
              false,
            );
          }
        }
      },
      [
        organizationId,
      ],
    );

  useEffect(
    () => {
      const timer =
        window.setTimeout(
          () => {
            void load();
          },
          0,
        );

      return () => {
        window.clearTimeout(
          timer,
        );
      };
    },
    [
      load,
    ],
  );

  const statusMaximum =
    useMemo(
      () =>
        Math.max(
          1,
          ...(
            dashboard
              ?.projectStatus
            ?? []
          ).map(
            (
              item,
            ) =>
              item.count,
          ),
        ),
      [
        dashboard,
      ],
    );

  if (
    authLoading
    || !user
  ) {
    return (
      <main
        className="appLoading"
      >
        Loading BuildFlow…
      </main>
    );
  }

  return (
    <main
      className={`appShell ${styles.dashboardShell}`}
    >
      <AppSidebar
        active="overview"
      />

      <section
        className={
          styles.content
        }
      >

        <header
          className={
            styles.dashboardHeader
          }
        >
          <div
            className={
              styles.headerIdentity
            }
          >
            <span
              className={
                styles.organizationName
              }
            >
              {
                currentOrganization
                  ?.name
                ?? "Current organization"
              }
            </span>

            <h1>
              Overview
            </h1>

            <p
              className={
                styles.headerDescription
              }
            >
              Project activity, financial summaries and upcoming deadlines.
            </p>

            <div
              className={
                styles.scopeMetadata
              }
            >
              <span>
                Scope:{" "}
                {
                  currentOrganization
                    ?.name
                  ?? "Current organization"
                }
              </span>

              {
                retrievedAt
                  ? (
                      <span>
                        Retrieved:{" "}
                        {
                          formatRetrieved(
                            retrievedAt,
                          )
                        }
                      </span>
                    )
                  : null
              }
            </div>
          </div>

          <div
            className={
              styles.headerActions
            }
          >
            {
              canOpenReports
                ? (
                    <Link
                      className={
                        styles.outlineAction
                      }
                      href="/app/reports"
                    >
                      Reports &amp; Exports
                    </Link>
                  )
                : null
            }

            <div
              className={
                styles.notificationAction
              }
            >
              <NotificationBell />
            </div>
          </div>
        </header>

        {
          !organizationId
            ? (
                <section
                  className={
                    styles.statePanel
                  }
                >
                  <strong>
                    No organization selected
                  </strong>

                  <p>
                    Select an available organization to review its dashboard.
                  </p>
                </section>
              )
            : null
        }

        {
          organizationId
          && dashboardLoading
            ? (
                <section
                  className={
                    styles.statePanel
                  }
                  role="status"
                  aria-live="polite"
                >
                  <strong>
                    Loading dashboard
                  </strong>

                  <p>
                    Retrieving the current organization scope.
                  </p>
                </section>
              )
            : null
        }

        {
          organizationId
          && !dashboardLoading
          && error
            ? (
                <section
                  className={`${styles.statePanel} ${styles.errorPanel}`}
                  role="alert"
                >
                  <div>
                    <strong>
                      Dashboard unavailable
                    </strong>

                    <p>
                      {
                        error
                      }
                    </p>
                  </div>

                  <button
                    type="button"
                    className={
                      styles.retryButton
                    }
                    onClick={() => {
                      void load();
                    }}
                  >
                    Try again
                  </button>
                </section>
              )
            : null
        }

        {
          dashboard
            ? (
                <div
                  className={
                    styles.dashboardBody
                  }
                >
                  <AttentionPanel
                    organizationId={organizationId}
                    currency={dashboard.currency}
                  />

                  {/* ======================================================
                      D02 — Operational summary
                      ====================================================== */}

                  <section
                    className={
                      styles.dashboardGroup
                    }
                    aria-labelledby="operations-heading"
                  >
                    <header
                      className={
                        styles.sectionHeader
                      }
                    >
                      <div>
                        <h2
                          id="operations-heading"
                        >
                          Current operations
                        </h2>

                        <p>
                          Current project and delivery conditions in this scope.
                        </p>
                      </div>
                    </header>

                    <div
                      className={
                        styles.metricGrid
                      }
                    >
                      <MetricCard
                        label="Active projects"
                        value={
                          dashboard
                            .activeProjects
                        }
                        qualifier="Projects classified as active"
                      />

                      <MetricCard
                        label="Projects at risk"
                        value={
                          dashboard
                            .projectsAtRisk
                        }
                        qualifier="Current backend risk classification"
                      />

                      <MetricCard
                        label="Overdue tasks"
                        value={
                          dashboard
                            .overdueTasks
                        }
                        qualifier="Tasks currently recorded as overdue"
                      />

                      <MetricCard
                        label="Open incidents"
                        value={
                          dashboard
                            .openIncidents
                        }
                        qualifier="Incidents currently recorded as open"
                      />
                    </div>

                    <div
                      className={
                        styles.operationsGrid
                      }
                    >
                      <article
                        className={
                          styles.panel
                        }
                      >
                        <header
                          className={
                            styles.panelHeader
                          }
                        >
                          <div>
                            <h3>
                              Project status
                            </h3>

                            <p>
                              Counts in the current dashboard scope
                            </p>
                          </div>

                          <Link
                            className={
                              styles.inlineAction
                            }
                            href="/app/projects"
                          >
                            View projects
                          </Link>
                        </header>

                        {
                          dashboard
                            .projectStatus
                            .length ===
                            0
                            ? (
                                <div
                                  className={
                                    styles.compactEmptyState
                                  }
                                >
                                  <strong>
                                    No projects in this view
                                  </strong>

                                  <span>
                                    Projects will appear here when they are available in this scope.
                                  </span>
                                </div>
                              )
                            : (
                                <div
                                  className={
                                    styles.statusList
                                  }
                                >
                                  {
                                    dashboard
                                      .projectStatus
                                      .map(
                                        (
                                          item,
                                        ) => (
                                          <div
                                            className={
                                              styles.statusRow
                                            }
                                            key={
                                              item.status
                                            }
                                          >
                                            <div
                                              className={
                                                styles.statusIdentity
                                              }
                                            >
                                              <span>
                                                {
                                                  humanize(
                                                    item.status,
                                                  )
                                                }
                                              </span>

                                              <strong>
                                                {
                                                  item.count
                                                }
                                              </strong>
                                            </div>

                                            <div
                                              className={
                                                styles.statusTrack
                                              }
                                              aria-hidden="true"
                                            >
                                              <span
                                                style={{
                                                  width:
                                                    item.count ===
                                                      0
                                                      ? "0%"
                                                      : `${
                                                          (
                                                            item.count
                                                            / statusMaximum
                                                          )
                                                          * 100
                                                        }%`,
                                                }}
                                              />
                                            </div>
                                          </div>
                                        ),
                                      )
                                  }
                                </div>
                              )
                        }
                      </article>

                      <article
                        className={
                          styles.panel
                        }
                      >
                        <header
                          className={
                            styles.panelHeader
                          }
                        >
                          <div>
                            <h3>
                              Upcoming deadlines
                            </h3>

                            <p>
                              Next 30 days
                            </p>
                          </div>
                        </header>

                        {
                          dashboard
                            .upcomingDeadlines
                            .length ===
                            0
                            ? (
                                <div
                                  className={
                                    styles.compactEmptyState
                                  }
                                >
                                  <strong>
                                    No upcoming deadlines
                                  </strong>

                                  <span>
                                    No recorded deadlines fall within the next 30 days for this scope.
                                  </span>
                                </div>
                              )
                            : (
                                <div
                                  className={
                                    styles.deadlineList
                                  }
                                >
                                  {
                                    dashboard
                                      .upcomingDeadlines
                                      .map(
                                        (
                                          deadline,
                                        ) => (
                                          <Link
                                            className={
                                              styles.deadlineRow
                                            }
                                            key={`${deadline.type}-${deadline.id}`}
                                            href={`/app/projects/${deadline.projectId}`}
                                          >
                                            <span
                                              className={
                                                styles.deadlineType
                                              }
                                            >
                                              {
                                                humanize(
                                                  deadline.type,
                                                )
                                              }
                                            </span>

                                            <div
                                              className={
                                                styles.deadlineIdentity
                                              }
                                            >
                                              <strong>
                                                {
                                                  deadline.title
                                                }
                                              </strong>

                                              <span>
                                                {
                                                  deadline
                                                    .projectName
                                                }
                                              </span>
                                            </div>

                                            <time
                                              className={
                                                styles.deadlineDate
                                              }
                                              dateTime={
                                                deadline
                                                  .dueDate
                                                ?? undefined
                                              }
                                            >
                                              {
                                                formatDeadline(
                                                  deadline
                                                    .dueDate,
                                                )
                                              }
                                            </time>
                                          </Link>
                                        ),
                                      )
                                  }
                                </div>
                              )
                        }
                      </article>
                    </div>
                  </section>

                  {/* ======================================================
                      D04 — Financial overview
                      ====================================================== */}

                  <section
                    className={
                      styles.dashboardGroup
                    }
                    aria-labelledby="financial-heading"
                  >
                    <header
                      className={
                        styles.sectionHeader
                      }
                    >
                      <div>
                        <h2
                          id="financial-heading"
                        >
                          Financial overview
                        </h2>

                        <p>
                          Financial aggregates returned for the current dashboard scope.
                        </p>
                      </div>
                    </header>

                    <div
                      className={
                        styles.metricGrid
                      }
                    >
                      <MetricCard
                        label="Contract value"
                        value={
                          money(
                            dashboard
                              .totalContractValue,
                            dashboard
                              .currency,
                          )
                        }
                        qualifier="Current dashboard aggregate"
                      />

                      <MetricCard
                        label="Forecast revenue"
                        value={
                          money(
                            dashboard
                              .forecastRevenue,
                            dashboard
                              .currency,
                          )
                        }
                        qualifier="Current dashboard aggregate"
                      />

                      <MetricCard
                        label="Actual costs"
                        value={
                          money(
                            dashboard
                              .actualCosts,
                            dashboard
                              .currency,
                          )
                        }
                        qualifier="Current dashboard aggregate"
                      />

                      <MetricCard
                        label="Outstanding invoices"
                        value={
                          money(
                            dashboard
                              .outstandingInvoices,
                            dashboard
                              .currency,
                          )
                        }
                        qualifier="Current dashboard aggregate"
                      />
                    </div>

                    <ReceivablesPanel
                      organizationId={organizationId}
                      currency={dashboard.currency}
                    />

                    <div
                      className={
                        styles.chartGrid
                      }
                    >
                      <MonthlyChart
                        title="Monthly revenue"
                        values={
                          dashboard
                            .monthlyRevenue
                        }
                        currency={
                          dashboard
                            .currency
                        }
                        emptyMessage="No revenue records for this period."
                        tone="revenue"
                      />

                      <MonthlyChart
                        title="Monthly expenses"
                        values={
                          dashboard
                            .monthlyExpenses
                        }
                        currency={
                          dashboard
                            .currency
                        }
                        emptyMessage="No expense records for this period."
                        tone="expenses"
                      />
                    </div>

                    <article
                      className={
                        styles.panel
                      }
                    >
                      <header
                        className={
                          styles.panelHeader
                        }
                      >
                        <div>
                          <h3>
                            Project budget performance
                          </h3>

                          <p>
                            Existing project budget variance calculations
                          </p>
                        </div>
                      </header>

                      {
                        dashboard
                          .budgetPerformance
                          .length ===
                          0
                          ? (
                              <div
                                className={
                                  styles.compactEmptyState
                                }
                              >
                                <strong>
                                  No project budget records
                                </strong>

                                <span>
                                  Project budget results will appear here when they are available in this scope.
                                </span>
                              </div>
                            )
                          : (
                              <div
                                className={
                                  styles.tableScroller
                                }
                              >
                                <table
                                  className={
                                    styles.budgetTable
                                  }
                                >
                                  <thead>
                                    <tr>
                                      <th scope="col">
                                        Project
                                      </th>

                                      <th
                                        scope="col"
                                        className={
                                          styles.numeric
                                        }
                                      >
                                        Budget variance
                                      </th>

                                      <th
                                        scope="col"
                                        className={
                                          styles.numeric
                                        }
                                      >
                                        Variance %
                                      </th>
                                    </tr>
                                  </thead>

                                  <tbody>
                                    {
                                      dashboard
                                        .budgetPerformance
                                        .map(
                                          (
                                            item,
                                          ) => (
                                            <tr
                                              key={
                                                item.projectId
                                              }
                                            >
                                              <td
                                                data-label="Project"
                                              >
                                                <Link
                                                  className={
                                                    styles.projectLink
                                                  }
                                                  href={`/app/projects/${item.projectId}`}
                                                >
                                                  <strong>
                                                    {
                                                      item.projectName
                                                    }
                                                  </strong>

                                                  <span>
                                                    {
                                                      item.projectNumber
                                                    }
                                                  </span>
                                                </Link>
                                              </td>

                                              <td
                                                className={
                                                  styles.numeric
                                                }
                                                data-label="Budget variance"
                                              >
                                                {
                                                  money(
                                                    item.variance,
                                                    dashboard
                                                      .currency,
                                                  )
                                                }
                                              </td>

                                              <td
                                                className={
                                                  styles.numeric
                                                }
                                                data-label="Variance %"
                                              >
                                                {
                                                  Number.isFinite(
                                                    item
                                                      .variancePercentage,
                                                  )
                                                    ? `${item.variancePercentage.toFixed(
                                                        1,
                                                      )}%`
                                                    : "Calculation unavailable"
                                                }
                                              </td>
                                            </tr>
                                          ),
                                        )
                                    }
                                  </tbody>
                                </table>
                              </div>
                            )
                      }
                    </article>
                  </section>
                </div>
              )
            : null
        }
      </section>
    </main>
  );
}
