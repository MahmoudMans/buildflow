"use client";

import {
  useCallback,
  useEffect,
  useState,
} from "react";

import {
  useParams,
} from "next/navigation";

import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  ProjectWorkspaceNav,
} from "@/components/ProjectWorkspaceNav";

import {
  ProjectReportActions,
} from "@/components/ProjectReportActions";

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
  ProjectDashboard,
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

function Bars({
  values,
}: {
  values: DashboardMonthlyValue[];
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
    <div className="analyticsBars compact">
      {values.map((item) => (
        <div
          className="analyticsBarColumn"
          key={item.month}
        >
          <div className="analyticsBarTrack">
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
            {item.month.slice(5)}
          </span>
        </div>
      ))}
    </div>
  );
}

export default function ProjectDashboardPage() {
  const params = useParams();

  const projectId =
    String(params.projectId);

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
      ProjectDashboard | null
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
              ProjectDashboard
            >(
              `/projects/${projectId}/dashboard`,
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
                : "Unable to load project dashboard."
          );
        }
      },
      [
        organizationId,
        projectId,
      ]
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

  if (!dashboard) {
    return (
      <main className="appShell">
        <AppSidebar active="projects" />

        <section className="appContent">
          {error ? (
            <div className="formError moduleError">
              {error}
            </div>
          ) : (
            <div className="emptyState">
              Loading project dashboard...
            </div>
          )}
        </section>
      </main>
    );
  }

  const healthClass =
    dashboard.health.status
      .toLowerCase()
      .replace("_", "-");

  const revisedBudget =
    Math.max(
      1,
      dashboard.budgetUtilization
        .revisedBudget
    );

  return (
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent">
        <header className="appHeader projectDashboardHeader">
          <div>
            <p>
              {dashboard.projectNumber}
            </p>

            <h1>
              {dashboard.projectName}
            </h1>

            <span>
              {dashboard.status}
              {" · "}
              {dashboard.progressPercentage.toFixed(
                0
              )}
              % complete
            </span>
          </div>

          <div
            className={`healthBadge ${healthClass}`}
          >
            {dashboard.health.status.replace(
              "_",
              " "
            )}
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="overview"
        />

        <ProjectReportActions
          organizationId={organizationId ?? ""}
          projectId={projectId}
          projectNumber={dashboard.projectNumber}
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        {dashboard.health.reasons.length >
          0 && (
          <section className="healthReasons">
            {dashboard.health.reasons.map(
              (reason) => (
                <span key={reason}>
                  {reason}
                </span>
              )
            )}
          </section>
        )}

        <section className="projectOverviewDetails">
          <article>
            <span>
              Start Date
            </span>
            <strong>
              {dashboard.startDate
                ?? "—"}
            </strong>
          </article>

          <article>
            <span>
              Expected Completion
            </span>
            <strong>
              {dashboard.expectedCompletion
                ?? "—"}
            </strong>
          </article>

          <article>
            <span>
              Project Manager
            </span>
            <strong>
              {dashboard.projectManager
                ?? "—"}
            </strong>
          </article>

          <article>
            <span>
              Progress
            </span>
            <strong>
              {dashboard.progressPercentage.toFixed(
                0
              )}
              %
            </strong>
          </article>
        </section>

        <section className="dashboardMetricGrid projectFinancialGrid">
          <article>
            <span>
              Contract Value
            </span>
            <strong>
              {money(
                dashboard.financial
                  .revisedContractValue,
                dashboard.currency
              )}
            </strong>
          </article>

          <article>
            <span>
              Original Budget
            </span>
            <strong>
              {money(
                dashboard.financial
                  .originalBudget,
                dashboard.currency
              )}
            </strong>
          </article>

          <article>
            <span>
              Revised Budget
            </span>
            <strong>
              {money(
                dashboard.financial
                  .revisedBudget,
                dashboard.currency
              )}
            </strong>
          </article>

          <article>
            <span>
              Actual Cost
            </span>
            <strong>
              {money(
                dashboard.financial
                  .actualCost,
                dashboard.currency
              )}
            </strong>
          </article>

          <article>
            <span>
              Committed Cost
            </span>
            <strong>
              {money(
                dashboard.financial
                  .committedCost,
                dashboard.currency
              )}
            </strong>
          </article>

          <article>
            <span>
              Forecast Cost
            </span>
            <strong>
              {money(
                dashboard.financial
                  .forecastCost,
                dashboard.currency
              )}
            </strong>
          </article>

          <article>
            <span>
              Forecast Profit
            </span>
            <strong
              className={
                dashboard.financial
                    .forecastProfit <
                  0
                  ? "negativeValue"
                  : "positiveValue"
              }
            >
              {money(
                dashboard.financial
                  .forecastProfit,
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
        </section>

        <section className="projectOperationalGrid">
          <article>
            <span>
              Open Tasks
            </span>
            <strong>
              {dashboard.openTasks}
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
              Upcoming Milestones
            </span>
            <strong>
              {dashboard.upcomingMilestones}
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
                  COST
                </span>
                <h2>
                  Cost Evolution
                </h2>
              </div>
            </header>

            <Bars
              values={
                dashboard.costEvolution
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
                  Budget Utilization
                </h2>
              </div>
            </header>

            <div className="budgetUtilization">
              <div>
                <span>
                  Actual
                </span>
                <strong>
                  {money(
                    dashboard
                      .budgetUtilization
                      .actualCost,
                    dashboard.currency
                  )}
                </strong>
              </div>

              <div className="horizontalTrack">
                <div
                  style={{
                    width: `${Math.min(
                      100,
                      (dashboard
                        .budgetUtilization
                        .actualCost /
                        revisedBudget) *
                        100
                    )}%`,
                  }}
                />
              </div>

              <div>
                <span>
                  Forecast
                </span>
                <strong>
                  {money(
                    dashboard
                      .budgetUtilization
                      .forecastCost,
                    dashboard.currency
                  )}
                </strong>
              </div>

              <div className="horizontalTrack">
                <div
                  style={{
                    width: `${Math.min(
                      100,
                      (dashboard
                        .budgetUtilization
                        .forecastCost /
                        revisedBudget) *
                        100
                    )}%`,
                  }}
                />
              </div>
            </div>
          </article>
        </section>

        <section className="dashboardTwoColumn">
          <article className="dashboardPanel">
            <header>
              <div>
                <span>
                  TASKS
                </span>
                <h2>
                  Task Status
                </h2>
              </div>
            </header>

            <div className="statusAnalytics">
              {dashboard.taskStatus.length ===
                0 && (
                <div className="emptyState">
                  No tasks yet.
                </div>
              )}

              {dashboard.taskStatus.map(
                (item) => (
                  <div key={item.status}>
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
                  </div>
                )
              )}
            </div>
          </article>

          <article className="dashboardPanel">
            <header>
              <div>
                <span>
                  EXPENSES
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
            />
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
                <article
                  key={`${deadline.type}-${deadline.id}`}
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
                      {deadline.priority
                        ?? "MEDIUM"}
                    </span>
                  </div>

                  <time>
                    {deadline.dueDate
                      ?? "—"}
                  </time>
                </article>
              )
            )}
          </div>
        </section>
      </section>
    </main>
  );
}
