"use client";

import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import {
  useParams,
} from "next/navigation";

import { AppSidebar } from "@/components/AppSidebar";
import { ProjectWorkspaceNav } from "@/components/ProjectWorkspaceNav";
import { useAuth } from "@/components/AuthProvider";

import {
  ApiClientError,
  apiDelete,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  BudgetView,
  Expense,
  FinancialSummary,
  ProjectMember,
  ProjectView,
} from "@/lib/types";

export default function ExpensesPage() {
  const params = useParams();

  const projectId =
    String(params.projectId);

  const {
    user,
    organizations,
    loading,
  } = useAuth();

  const [organizationId, setOrganizationId] =
    useState("");

  const [project, setProject] =
    useState<ProjectView | null>(
      null
    );

  const [budget, setBudget] =
    useState<BudgetView | null>(
      null
    );

  const [summary, setSummary] =
    useState<FinancialSummary | null>(
      null
    );

  const [expenses, setExpenses] =
    useState<Expense[]>([]);

  const [
    projectMembers,
    setProjectMembers,
  ] = useState<ProjectMember[]>([]);

  const [categoryId, setCategoryId] =
    useState("");

  const [description, setDescription] =
    useState("");

  const [expenseDate, setExpenseDate] =
    useState(
      new Date()
        .toISOString()
        .slice(0, 10)
    );

  const [netAmount, setNetAmount] =
    useState("");

  const [taxAmount, setTaxAmount] =
    useState("0");

  const [error, setError] =
    useState("");

  useEffect(() => {
    if (
      organizations.length === 0
    ) {
      return;
    }

    const stored =
      localStorage.getItem(
        "buildflow.organizationId"
      );

    setOrganizationId(
      stored
      && organizations.some(
        (organization) =>
          organization.id === stored
      )
        ? stored
        : organizations[0].id
    );
  }, [organizations]);

  useEffect(() => {
    if (
      organizationId
      && projectId
    ) {
      loadData();
    }
  }, [
    organizationId,
    projectId,
  ]);

  async function loadData() {
    try {
      const [
        projectResult,
        budgetResult,
        summaryResult,
        expenseResult,
        memberResult,
      ] = await Promise.all([
        apiGet<ProjectView>(
          `/projects/${projectId}`,
          organizationId
        ),

        apiGet<BudgetView>(
          `/projects/${projectId}/budget`,
          organizationId
        ),

        apiGet<FinancialSummary>(
          `/projects/${projectId}/financial-summary`,
          organizationId
        ),

        apiGet<Expense[]>(
          `/projects/${projectId}/expenses`,
          organizationId
        ),

        apiGet<ProjectMember[]>(
          `/projects/${projectId}/members`,
          organizationId
        ),
      ]);

      setProject(projectResult);
      setBudget(budgetResult);
      setSummary(summaryResult);
      setExpenses(expenseResult);
      setProjectMembers(memberResult);

      if (
        !categoryId
        && budgetResult.categories.length > 0
      ) {
        setCategoryId(
          budgetResult.categories[0].id
        );
      }
    } catch (caught) {
      handleError(caught);
    }
  }

  const organization =
    organizations.find(
      (entry) =>
        entry.id === organizationId
    );

  const currentProjectMember =
    projectMembers.find(
      (member) =>
        member.userId === user?.id
    );

  const canCreate =
    organization?.role === "OWNER"
    || organization?.role === "ADMIN"
    || organization?.role === "ACCOUNTANT"
    || currentProjectMember?.projectRole
      === "PROJECT_MANAGER"
    || currentProjectMember?.projectRole
      === "SITE_MANAGER";

  const canApprove =
    organization?.role === "OWNER"
    || organization?.role === "ADMIN"
    || organization?.role === "ACCOUNTANT";

  async function createExpense(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPost(
        `/projects/${projectId}/expenses`,
        {
          budgetCategoryId:
            categoryId,
          description,
          expenseDate,
          netAmount:
            Number(netAmount),
          taxAmount:
            Number(taxAmount),
        },
        organizationId
      );

      setDescription("");
      setNetAmount("");
      setTaxAmount("0");

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function action(
    expenseId: string,
    path: string,
    body?: unknown
  ) {
    try {
      await apiPost(
        `/expenses/${expenseId}/${path}`,
        body,
        organizationId
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function reject(
    expenseId: string
  ) {
    const reason =
      window.prompt(
        "Reason for rejecting this expense:"
      );

    if (reason === null) {
      return;
    }

    await action(
      expenseId,
      "reject",
      {
        reason,
      }
    );
  }

  async function voidExpense(
    expenseId: string
  ) {
    const reason =
      window.prompt(
        "Reason for voiding this approved expense:"
      );

    if (
      reason === null
      || !reason.trim()
    ) {
      return;
    }

    await action(
      expenseId,
      "void",
      {
        reason,
      }
    );
  }

  async function deleteDraft(
    expenseId: string
  ) {
    if (
      !window.confirm(
        "Delete this draft expense?"
      )
    ) {
      return;
    }

    try {
      await apiDelete(
        `/expenses/${expenseId}`,
        organizationId
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  function handleError(
    caught: unknown
  ) {
    setError(
      caught instanceof ApiClientError
        ? caught.message
        : "The expense operation failed."
    );
  }

  function money(
    value: number
  ) {
    return new Intl.NumberFormat(
      "en-US",
      {
        style: "currency",
        currency:
          project?.currency ?? "EUR",
      }
    ).format(value);
  }

  if (
    loading
    || !user
    || !project
    || !budget
    || !summary
  ) {
    return (
      <main className="appLoading">
        {error || "Loading..."}
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>
              {project.projectNumber}
            </p>

            <h1>
              Project expenses
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="expenses"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="financialMetricGrid">
          <article>
            <span>
              Revised budget
            </span>

            <strong>
              {money(
                Number(
                  summary.revisedBudget
                )
              )}
            </strong>
          </article>

          <article>
            <span>
              Actual cost
            </span>

            <strong>
              {money(
                Number(
                  summary.actualCost
                )
              )}
            </strong>
          </article>

          <article>
            <span>
              Remaining budget
            </span>

            <strong>
              {money(
                Number(
                  summary.budgetRemaining
                )
              )}
            </strong>
          </article>

          <article>
            <span>
              Expense records
            </span>

            <strong>
              {expenses.length}
            </strong>
          </article>
        </section>

        <section className="expensePageLayout">
          <div className="expenseTableCard">
            <div className="expenseTableHeader">
              <span>
                Expense
              </span>

              <span>
                Date
              </span>

              <span>
                Net
              </span>

              <span>
                Tax
              </span>

              <span>
                Total
              </span>

              <span>
                Status
              </span>

              <span>
                Actions
              </span>
            </div>

            {expenses.length ===
              0 && (
              <div className="emptyState">
                No expenses yet.
              </div>
            )}

            {expenses.map(
              (expense) => (
                <div
                  className="expenseTableRow"
                  key={expense.id}
                >
                  <div>
                    <strong>
                      {expense.expenseNumber}
                    </strong>

                    <span>
                      {expense.description}
                    </span>
                  </div>

                  <span>
                    {expense.expenseDate}
                  </span>

                  <span>
                    {money(
                      Number(
                        expense.netAmount
                      )
                    )}
                  </span>

                  <span>
                    {money(
                      Number(
                        expense.taxAmount
                      )
                    )}
                  </span>

                  <strong>
                    {money(
                      Number(
                        expense.totalAmount
                      )
                    )}
                  </strong>

                  <span
                    className={`expenseStatus expenseStatus-${expense.status.toLowerCase()}`}
                  >
                    {expense.status}
                  </span>

                  <div className="expenseActions">
                    {expense.status ===
                      "DRAFT" && (
                      <>
                        <button
                          type="button"
                          onClick={() =>
                            action(
                              expense.id,
                              "submit"
                            )
                          }
                        >
                          Submit
                        </button>

                        <button
                          type="button"
                          onClick={() =>
                            deleteDraft(
                              expense.id
                            )
                          }
                        >
                          Delete
                        </button>
                      </>
                    )}

                    {expense.status ===
                      "SUBMITTED"
                      && canApprove && (
                      <>
                        <button
                          type="button"
                          onClick={() =>
                            action(
                              expense.id,
                              "approve"
                            )
                          }
                        >
                          Approve
                        </button>

                        <button
                          type="button"
                          onClick={() =>
                            reject(
                              expense.id
                            )
                          }
                        >
                          Reject
                        </button>
                      </>
                    )}

                    {expense.status ===
                      "APPROVED"
                      && canApprove && (
                      <button
                        type="button"
                        onClick={() =>
                          voidExpense(
                            expense.id
                          )
                        }
                      >
                        Void
                      </button>
                    )}
                  </div>
                </div>
              )
            )}
          </div>

          {canCreate && (
            <aside className="moduleFormCard">
              <h2>
                Add expense
              </h2>

              <form
                className="authForm"
                onSubmit={createExpense}
              >
                <label>
                  Budget category
                  <select
                    required
                    value={categoryId}
                    onChange={(event) =>
                      setCategoryId(
                        event.target.value
                      )
                    }
                  >
                    <option value="">
                      Select category
                    </option>

                    {budget.categories.map(
                      (category) => (
                        <option
                          key={
                            category.id
                          }
                          value={
                            category.id
                          }
                        >
                          {category.name}
                        </option>
                      )
                    )}
                  </select>
                </label>

                <label>
                  Description
                  <input
                    required
                    value={description}
                    onChange={(event) =>
                      setDescription(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Expense date
                  <input
                    type="date"
                    required
                    value={expenseDate}
                    onChange={(event) =>
                      setExpenseDate(
                        event.target.value
                      )
                    }
                  />
                </label>

                <div className="fieldRow">
                  <label>
                    Net amount
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      required
                      value={netAmount}
                      onChange={(event) =>
                        setNetAmount(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <label>
                    Tax amount
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      required
                      value={taxAmount}
                      onChange={(event) =>
                        setTaxAmount(
                          event.target.value
                        )
                      }
                    />
                  </label>
                </div>

                <button
                  className="authSubmit"
                  type="submit"
                  disabled={
                    !categoryId
                  }
                >
                  Create draft expense
                </button>
              </form>
            </aside>
          )}
        </section>
      </section>
    </main>
  );
}
