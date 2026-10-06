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
import { useOrganizationSelection } from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiDelete,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  BudgetView,
  FinancialSummary,
  ProjectMember,
  ProjectView,
} from "@/lib/types";
import { formatMoneyExact } from "@/lib/format";

export default function BudgetPage() {
  const params = useParams();

  const projectId =
    String(params.projectId);

  const {
    user,
    organizations,
    loading,
  } = useAuth();

  const organizationSelection =
    useOrganizationSelection(
      organizations
    );

  const organizationId =
    organizationSelection.organizationId;

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

  const [
    projectMembers,
    setProjectMembers,
  ] = useState<ProjectMember[]>([]);

  const [categoryName, setCategoryName] =
    useState("");

  const [
    categoryDescription,
    setCategoryDescription,
  ] = useState("");

  const [itemCategoryId, setItemCategoryId] =
    useState("");

  const [itemDescription, setItemDescription] =
    useState("");

  const [quantity, setQuantity] =
    useState("1");

  const [unitCost, setUnitCost] =
    useState("");

  const [notes, setNotes] =
    useState("");

  const [error, setError] =
    useState("");



  async function loadData() {
    try {
      const [
        projectResult,
        budgetResult,
        summaryResult,
        membersResult,
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

        apiGet<ProjectMember[]>(
          `/projects/${projectId}/members`,
          organizationId
        ),
      ]);

      setProject(projectResult);
      setBudget(budgetResult);
      setSummary(summaryResult);
      setProjectMembers(membersResult);

      if (
        !itemCategoryId
        && budgetResult.categories.length > 0
      ) {
        setItemCategoryId(
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

  const canEdit =
    !budget?.baselineLocked
    && (
      organization?.role === "OWNER"
      || organization?.role === "ADMIN"
      || currentProjectMember?.projectRole
        === "PROJECT_MANAGER"
    );

  async function createCategory(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPost(
        `/projects/${projectId}/budget/categories`,
        {
          name: categoryName,
          description:
            categoryDescription || null,
          sortOrder:
            budget?.categories.length ?? 0,
        },
        organizationId
      );

      setCategoryName("");
      setCategoryDescription("");

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function createItem(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!itemCategoryId) {
      return;
    }

    try {
      await apiPost(
        `/projects/${projectId}/budget/categories/${itemCategoryId}/items`,
        {
          description:
            itemDescription,
          quantity:
            Number(quantity),
          unitCost:
            Number(unitCost),
          notes:
            notes || null,
        },
        organizationId
      );

      setItemDescription("");
      setQuantity("1");
      setUnitCost("");
      setNotes("");

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function deleteItem(
    itemId: string
  ) {
    if (
      !window.confirm(
        "Delete this budget item?"
      )
    ) {
      return;
    }

    try {
      await apiDelete(
        `/projects/${projectId}/budget/items/${itemId}`,
        organizationId
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function deleteCategory(
    categoryId: string
  ) {
    if (
      !window.confirm(
        "Delete this budget category and its items?"
      )
    ) {
      return;
    }

    try {
      await apiDelete(
        `/projects/${projectId}/budget/categories/${categoryId}`,
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
        : "The budget operation failed."
    );
  }

  function money(value: number) {
    return formatMoneyExact(
      value,
      project?.currency ?? "EUR",
    );
  }

    /* eslint-disable react-hooks/exhaustive-deps */
useEffect(() => {
    // BUILDFLOW_ASYNC_LOAD_EFFECT
    const timer =
      window.setTimeout(
        () => {
        if (
          organizationId
          && projectId
        ) {
          loadData();
        }
        },
        0
      );

    return () => {
      window.clearTimeout(
        timer
      );
    };
  }, [
    organizationId,
    projectId,
  ]);
  /* eslint-enable react-hooks/exhaustive-deps */

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
              Project budget
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>

          <div className="budgetState">
            <span>
              Budget baseline
            </span>

            <strong>
              {budget.baselineLocked
                ? "LOCKED"
                : "DRAFT"}
            </strong>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="budget"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        {budget.baselineLocked && (
          <div className="budgetLockedBanner">
            Original budget is locked because
            this project has been activated.
            Future adjustments will be handled
            through change orders.
          </div>
        )}

        <section className="financialMetricGrid">
          <article>
            <span>
              Planned budget
            </span>

            <strong>
              {money(
                Number(
                  summary.plannedBudget
                )
              )}
            </strong>
          </article>

          <article>
            <span>
              Original budget
            </span>

            <strong>
              {money(
                Number(
                  summary.originalBudget
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
              Remaining
            </span>

            <strong>
              {money(
                Number(
                  summary.budgetRemaining
                )
              )}
            </strong>
          </article>
        </section>

        <section className="budgetPageLayout">
          <div className="budgetCategories">
            {budget.categories.length ===
              0 && (
              <div className="emptyState budgetEmpty">
                No budget categories yet.
              </div>
            )}

            {budget.categories.map(
              (category) => (
                <article
                  className="budgetCategoryCard"
                  key={category.id}
                >
                  <header>
                    <div>
                      <h2>
                        {category.name}
                      </h2>

                      <p>
                        {category.description
                          || "No description"}
                      </p>
                    </div>

                    <div className="budgetCategoryTotal">
                      <span>
                        Planned
                      </span>

                      <strong>
                        {money(
                          Number(
                            category.plannedAmount
                          )
                        )}
                      </strong>
                    </div>
                  </header>

                  <div className="budgetItemTable">
                    <div className="budgetItemHeader">
                      <span>
                        Description
                      </span>

                      <span>
                        Qty
                      </span>

                      <span>
                        Unit cost
                      </span>

                      <span>
                        Total
                      </span>

                      <span />
                    </div>

                    {category.items.length ===
                      0 && (
                      <div className="budgetItemEmpty">
                        No items in this category.
                      </div>
                    )}

                    {category.items.map(
                      (item) => (
                        <div
                          className="budgetItemRow"
                          key={item.id}
                        >
                          <div>
                            <strong>
                              {item.description}
                            </strong>

                            {item.notes && (
                              <span>
                                {item.notes}
                              </span>
                            )}
                          </div>

                          <span>
                            {item.quantity}
                          </span>

                          <span>
                            {money(
                              Number(
                                item.unitCost
                              )
                            )}
                          </span>

                          <strong>
                            {money(
                              Number(
                                item.plannedAmount
                              )
                            )}
                          </strong>

                          <div>
                            {canEdit && (
                              <button
                                className="tableAction"
                                type="button"
                                onClick={() =>
                                  deleteItem(
                                    item.id
                                  )
                                }
                              >
                                Delete
                              </button>
                            )}
                          </div>
                        </div>
                      )
                    )}
                  </div>

                  {canEdit && (
                    <footer>
                      <button
                        className="tableAction danger"
                        type="button"
                        onClick={() =>
                          deleteCategory(
                            category.id
                          )
                        }
                      >
                        Delete category
                      </button>
                    </footer>
                  )}
                </article>
              )
            )}
          </div>

          {canEdit && (
            <aside className="budgetForms">
              <section className="moduleFormCard">
                <h2>
                  Add category
                </h2>

                <form
                  className="authForm"
                  onSubmit={createCategory}
                >
                  <label>
                    Name
                    <input
                      required
                      value={categoryName}
                      onChange={(event) =>
                        setCategoryName(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <label>
                    Description
                    <textarea
                      value={
                        categoryDescription
                      }
                      onChange={(event) =>
                        setCategoryDescription(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <button
                    className="authSubmit"
                    type="submit"
                  >
                    Add category
                  </button>
                </form>
              </section>

              <section className="moduleFormCard">
                <h2>
                  Add budget item
                </h2>

                <form
                  className="authForm"
                  onSubmit={createItem}
                >
                  <label>
                    Category
                    <select
                      required
                      value={
                        itemCategoryId
                      }
                      onChange={(event) =>
                        setItemCategoryId(
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
                      value={
                        itemDescription
                      }
                      onChange={(event) =>
                        setItemDescription(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <div className="fieldRow">
                    <label>
                      Quantity
                      <input
                        type="number"
                        min="0"
                        step="0.0001"
                        required
                        value={quantity}
                        onChange={(event) =>
                          setQuantity(
                            event.target.value
                          )
                        }
                      />
                    </label>

                    <label>
                      Unit cost
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        required
                        value={unitCost}
                        onChange={(event) =>
                          setUnitCost(
                            event.target.value
                          )
                        }
                      />
                    </label>
                  </div>

                  <label>
                    Notes
                    <textarea
                      value={notes}
                      onChange={(event) =>
                        setNotes(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <button
                    className="authSubmit"
                    type="submit"
                    disabled={
                      !itemCategoryId
                    }
                  >
                    Add budget item
                  </button>
                </form>
              </section>
            </aside>
          )}
        </section>
      </section>
    </main>
  );
}
