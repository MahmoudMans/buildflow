"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import Link from "next/link";

import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  NotificationBell,
} from "@/components/NotificationBell";

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

function money(
  value: number,
  currency: string
) {
  return new Intl.NumberFormat(
    undefined,
    {
      style: "currency",
      currency,
      maximumFractionDigits: 0,
    }
  ).format(value);
}

function shortMonth(
  value: string
) {
  const [year, month] =
    value.split("-");

  return new Date(
    Number(year),
    Number(month) - 1,
    1
  ).toLocaleDateString(
    undefined,
    {
      month: "short",
    }
  );
}

function Bars({
  values,
  currency,
}: {
  values: DashboardMonthlyValue[];
  currency: string;
}) {
  const maximum =
    Math.max(
      1,
      ...values.map(
        (item) =>
          Math.abs(item.value)
      )
    );

  return (
    <div className="analyticsBars">
      {values.map((item) => (
        <div
          className="analyticsBarColumn"
          key={item.month}
        >
          <div
            className="analyticsBarTrack"
            title={`${item.month}: ${money(item.value, currency)}`}
          >
            <div
              className="analyticsBarFill"
              style={{
                height: `${Math.max(
                  3,
                  (Math.abs(item.value) /
                    maximum) *
                    100
                )}%`,
              }}
            />
          </div>

          <span>
            {shortMonth(item.month)}
          </span>
        </div>
      ))}
    </div>
  );
}

export default function DashboardPage() {
  const {
    user,
    organizations,
    loading,
  } = useAuth();

  const selection =
    useOrganizationSelection(
      organizations
    );

  const organizationId =
    selection.organizationId;

  const [dashboard, setDashboard] =
    useState<
      OrganizationDashboard | null
    >(null);

  const [error, setError] =
    useState("");

  const load =
    useCallback(
      async () => {
        if (!organizationId) {
          return;
        }

        try {
          setError("");

          const result =
            await apiGet<
              OrganizationDashboard
            >(
              "/dashboard",
              organizationId
            );

          setDashboard(result);
        } catch (caught) {
          setError(
            caught
              instanceof ApiClientError
              ? caught.message
              : caught
                instanceof Error
                ? caught.message
                : "Unable to load dashboard."
          );
        }
      },
      [organizationId]
    );

  useEffect(() => {
    if (!organizationId) {
      return;
    }

    const timer =
      window.setTimeout(
        () => {
          void load();
        },
        0
      );

    return () => {
      window.clearTimeout(timer);
    };
  }, [
    organizationId,
    load,
  ]);

  const statusMaximum =
    useMemo(
      () =>
        Math.max(
          1,
          ...(
            dashboard?.projectStatus
              ?? []
          ).map(
            (item) =>
              item.count
          )
        ),
      [dashboard]
    );

  if (
    loading
    || !user
  ) {
    return (
      <main className="appLoading">
        Loading...
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="overview" />

      <section className="appContent">
        <header className="appHeader dashboardHeader">
          <div>
            <p>
              ORGANIZATION ANALYTICS
            </p>

            <h1>
              Dashboard
            </h1>

            <span>
              Operational and financial
              performance from backend
              records.
            </span>
          </div>

          <div className="dashboardHeaderActions">
            <Link
              className="reportLinkButton"
              href="/app/reports"
            >
              Reports & Exports
            </Link>

            <NotificationBell />
          </div>
        </header>

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        {!dashboard && !error && (
          <div className="emptyState">
            Loading dashboard...
          </div>
        )}

        {dashboard && (
          <>
            <section className="dashboardMetricGrid">
              <article>
                <span>
                  Active Projects
                </span>
                <strong>
                  {dashboard.activeProjects}
                </strong>
              </article>

              <article>
                <span>
                  Contract Value
                </span>
                <strong>
                  {money(
                    dashboard.totalContractValue,
                    dashboard.currency
                  )}
                </strong>
              </article>

              <article>
                <span>
                  Forecast Revenue
                </span>
                <strong>
                  {money(
                    dashboard.forecastRevenue,
                    dashboard.currency
                  )}
                </strong>
              </article>

              <article>
                <span>
                  Actual Costs
                </span>
                <strong>
                  {money(
                    dashboard.actualCosts,
                    dashboard.currency
                  )}
                </strong>
              </article>

              <article>
                <span>
                  Outstanding Invoices
                </span>
                <strong>
                  {money(
                    dashboard.outstandingInvoices,
                    dashboard.currency
                  )}
                </strong>
              </article>

              <article>
                <span>
                  Projects At Risk
                </span>
                <strong>
                  {dashboard.projectsAtRisk}
                </strong>
              </article>

              <article>
                <span>
                  Overdue Tasks
                </span>
                <strong>
                  {dashboard.overdueTasks}
                </strong>
              </article>

              <article>
                <span>
                  Open Incidents
                </span>
                <strong>
                  {dashboard.openIncidents}
                </strong>
              </article>
            </section>

            <section className="dashboardTwoColumn">
              <article className="dashboardPanel">
                <header>
                  <div>
                    <span>
                      PORTFOLIO
                    </span>
                    <h2>
                      Project Status
                    </h2>
                  </div>
                </header>

                <div className="statusAnalytics">
                  {dashboard.projectStatus.map(
                    (item) => (
                      <div
                        key={item.status}
                      >
                        <div>
                          <span>
                            {item.status.replace(
                              "_",
                              " "
                            )}
                          </span>
                          <strong>
                            {item.count}
                          </strong>
                        </div>

                        <div className="horizontalTrack">
                          <div
                            style={{
                              width: `${
                                (item.count /
                                  statusMaximum) *
                                100
                              }%`,
                            }}
                          />
                        </div>
                      </div>
                    )
                  )}
                </div>
              </article>

              <article className="dashboardPanel">
                <header>
                  <div>
                    <span>
                      CASH
                    </span>
                    <h2>
                      Monthly Revenue
                    </h2>
                  </div>
                </header>

                <Bars
                  values={
                    dashboard.monthlyRevenue
                  }
                  currency={
                    dashboard.currency
                  }
                />
              </article>
            </section>

            <section className="dashboardTwoColumn">
              <article className="dashboardPanel">
                <header>
                  <div>
                    <span>
                      COST
                    </span>
                    <h2>
                      Monthly Expenses
                    </h2>
                  </div>
                </header>

                <Bars
                  values={
                    dashboard.monthlyExpenses
                  }
                  currency={
                    dashboard.currency
                  }
                />
              </article>

              <article className="dashboardPanel">
                <header>
                  <div>
                    <span>
                      BUDGET
                    </span>
                    <h2>
                      Budget Performance
                    </h2>
                  </div>
                </header>

                <div className="budgetPerformanceList">
                  {dashboard.budgetPerformance.length ===
                    0 && (
                    <div className="emptyState">
                      No project budgets yet.
                    </div>
                  )}

                  {dashboard.budgetPerformance.map(
                    (item) => (
                      <Link
                        key={item.projectId}
                        href={`/app/projects/${item.projectId}`}
                      >
                        <div>
                          <strong>
                            {item.projectName}
                          </strong>
                          <span>
                            {item.projectNumber}
                          </span>
                        </div>

                        <div>
                          <strong
                            className={
                              item.variance < 0
                                ? "negativeValue"
                                : "positiveValue"
                            }
                          >
                            {money(
                              item.variance,
                              dashboard.currency
                            )}
                          </strong>

                          <span>
                            {item.variancePercentage.toFixed(
                              1
                            )}
                            %
                          </span>
                        </div>
                      </Link>
                    )
                  )}
                </div>
              </article>
            </section>

            <section className="dashboardPanel">
              <header>
                <div>
                  <span>
                    NEXT 30 DAYS
                  </span>
                  <h2>
                    Upcoming Deadlines
                  </h2>
                </div>
              </header>

              <div className="dashboardDeadlineList">
                {dashboard.upcomingDeadlines.length ===
                  0 && (
                  <div className="emptyState">
                    No upcoming deadlines.
                  </div>
                )}

                {dashboard.upcomingDeadlines.map(
                  (deadline) => (
                    <Link
                      key={`${deadline.type}-${deadline.id}`}
                      href={`/app/projects/${deadline.projectId}`}
                    >
                      <span
                        className={`deadlineType ${deadline.type.toLowerCase()}`}
                      >
                        {deadline.type}
                      </span>

                      <div>
                        <strong>
                          {deadline.title}
                        </strong>
                        <span>
                          {deadline.projectName}
                        </span>
                      </div>

                      <time>
                        {deadline.dueDate
                          ? new Date(
                              `${deadline.dueDate}T00:00:00`
                            ).toLocaleDateString()
                          : "—"}
                      </time>
                    </Link>
                  )
                )}
              </div>
            </section>
          </>
        )}
      </section>
    </main>
  );
}
