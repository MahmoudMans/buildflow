"use client";

import {
  type FormEvent,
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
  useAuth,
} from "@/components/AuthProvider";

import {
  useOrganizationSelection,
} from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  ProjectView,
} from "@/lib/types";

import type {
  ChangeOrder,
  ChangeOrderFinancialSummary,
  ChangeOrderSummary,
} from "@/lib/change-order-types";
import { formatMoneyExact } from "@/lib/format";
import { ChangeOrderImpactPanel } from "@/components/ChangeOrderImpactPanel";

export default function ChangeOrdersPage() {
  const params = useParams();

  const projectId =
    String(
      params.projectId
    );

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

  const organization =
    organizations.find(
      (entry) =>
        entry.id
          === organizationId
    );

  const role =
    organization?.role;

  const canManage =
    role === "OWNER"
    || role === "ADMIN"
    || role === "PROJECT_MANAGER";

  const canDecide =
    role === "OWNER"
    || role === "ADMIN"
    || role === "CLIENT";

  const canSeeFinancials =
    role === "OWNER"
    || role === "ADMIN"
    || role === "ACCOUNTANT"
    || role === "PROJECT_MANAGER";

  const canSeeInternalImpact =
    role !== "CLIENT";

  const [project, setProject] =
    useState<ProjectView | null>(
      null
    );

  const [
    changeOrders,
    setChangeOrders,
  ] =
    useState<
      ChangeOrderSummary[]
    >([]);

  const [
    selectedChangeOrder,
    setSelectedChangeOrder,
  ] =
    useState<
      ChangeOrder | null
    >(null);

  const [
    financial,
    setFinancial,
  ] =
    useState<
      ChangeOrderFinancialSummary
      | null
    >(null);

  const [title, setTitle] =
    useState("");

  const [
    description,
    setDescription,
  ] =
    useState("");

  const [reason, setReason] =
    useState("");

  const [
    costImpact,
    setCostImpact,
  ] =
    useState("0");

  const [
    clientPriceImpact,
    setClientPriceImpact,
  ] =
    useState("0");

  const [
    scheduleImpactDays,
    setScheduleImpactDays,
  ] =
    useState("0");

  const [error, setError] =
    useState("");

  const handleError =
    useCallback(
      (
        caught: unknown
      ) => {
        setError(
          caught
            instanceof ApiClientError
            ? caught.message
            : "The change-order operation failed."
        );
      },
      []
    );

  const loadChangeOrder =
    useCallback(
      async (
        changeOrderId: string
      ) => {
        if (!organizationId) {
          return;
        }

        try {
          const result =
            await apiGet<ChangeOrder>(
              `/change-orders/${changeOrderId}`,
              organizationId
            );

          setSelectedChangeOrder(
            result
          );
        } catch (caught) {
          handleError(caught);
        }
      },
      [
        organizationId,
        handleError,
      ]
    );

  const loadData =
    useCallback(
      async (
        preferredChangeOrderId?: string
      ) => {
        if (
          !organizationId
          || !projectId
        ) {
          return;
        }

        try {
          setError("");

          const [
            projectResult,
            changeOrderResult,
          ] =
            await Promise.all([
              apiGet<ProjectView>(
                `/projects/${projectId}`,
                organizationId
              ),

              apiGet<
                ChangeOrderSummary[]
              >(
                `/projects/${projectId}/change-orders`,
                organizationId
              ),
            ]);

          setProject(
            projectResult
          );

          setChangeOrders(
            changeOrderResult
          );

          if (canSeeFinancials) {
            const financialResult =
              await apiGet<
                ChangeOrderFinancialSummary
              >(
                `/projects/${projectId}/change-orders/financial-summary`,
                organizationId
              );

            setFinancial(
              financialResult
            );
          } else {
            setFinancial(null);
          }

          if (
            changeOrderResult.length
              === 0
          ) {
            setSelectedChangeOrder(
              null
            );

            return;
          }

          const nextId =
            preferredChangeOrderId
            && changeOrderResult.some(
              (changeOrder) =>
                changeOrder.id
                  === preferredChangeOrderId
            )
              ? preferredChangeOrderId
              : changeOrderResult[0].id;

          await loadChangeOrder(
            nextId
          );
        } catch (caught) {
          handleError(caught);
        }
      },
      [
        organizationId,
        projectId,
        canSeeFinancials,
        loadChangeOrder,
        handleError,
      ]
    );

  useEffect(() => {
    if (
      !organizationId
      || !projectId
    ) {
      return;
    }

    const timer =
      window.setTimeout(
        () => {
          void loadData();
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
    loadData,
  ]);

  async function createChangeOrder(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      const result =
        await apiPost<ChangeOrder>(
          `/projects/${projectId}/change-orders`,
          {
            title,
            description:
              description || null,
            reason:
              reason || null,
            costImpact:
              Number(costImpact),
            clientPriceImpact:
              Number(
                clientPriceImpact
              ),
            scheduleImpactDays:
              Number(
                scheduleImpactDays
              ),
          },
          organizationId
        );

      setTitle("");
      setDescription("");
      setReason("");
      setCostImpact("0");
      setClientPriceImpact("0");
      setScheduleImpactDays("0");

      await loadData(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function transition(
    action: string
  ) {
    if (!selectedChangeOrder) {
      return;
    }

    try {
      const result =
        await apiPost<ChangeOrder>(
          `/change-orders/${selectedChangeOrder.id}/${action}`,
          undefined,
          organizationId
        );

      await loadData(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function cancelChangeOrder() {
    if (!selectedChangeOrder) {
      return;
    }

    const cancellationReason =
      window.prompt(
        "Reason for cancelling this change order:"
      );

    if (
      cancellationReason === null
      || !cancellationReason.trim()
    ) {
      return;
    }

    try {
      const result =
        await apiPost<ChangeOrder>(
          `/change-orders/${selectedChangeOrder.id}/cancel`,
          {
            reason:
              cancellationReason,
          },
          organizationId
        );

      await loadData(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  function money(value: number | null) {
    if (value === null) {
      return "Internal";
    }

    return formatMoneyExact(
      Number(value),
      project?.currency ?? "EUR",
    );
  }

  if (
    loading
    || !user
    || !project
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
              Change orders
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="changes"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        {financial && (
          <section className="changeOrderMetrics">
            <article>
              <span>
                Original budget
              </span>

              <strong>
                {money(
                  financial.originalBudget
                )}
              </strong>
            </article>

            <article>
              <span>
                Approved cost changes
              </span>

              <strong>
                {money(
                  financial
                    .approvedBudgetChangeOrders
                )}
              </strong>
            </article>

            <article>
              <span>
                Revised budget
              </span>

              <strong>
                {money(
                  financial.revisedBudget
                )}
              </strong>
            </article>

            <article>
              <span>
                Contract value
              </span>

              <strong>
                {money(
                  financial
                    .originalContractValue
                )}
              </strong>
            </article>

            <article>
              <span>
                Client changes
              </span>

              <strong>
                {money(
                  financial
                    .approvedClientChangeOrders
                )}
              </strong>
            </article>

            <article>
              <span>
                Revised contract
              </span>

              <strong>
                {money(
                  financial
                    .revisedContractValue
                )}
              </strong>
            </article>

            <article>
              <span>
                Forecast cost
              </span>

              <strong>
                {money(
                  financial.forecastCost
                )}
              </strong>
            </article>

            <article>
              <span>
                Forecast profit
              </span>

              <strong>
                {money(
                  financial.forecastProfit
                )}
              </strong>
            </article>
          </section>
        )}

        <section className="changeOrderGrid">
          <div className="changeOrderColumn">
            <div className="commercialSectionHeader">
              <div>
                <p>
                  SCOPE CONTROL
                </p>

                <h2>
                  Change requests
                </h2>
              </div>

              <span>
                {changeOrders.length}
              </span>
            </div>

            <div className="commercialList">
              {changeOrders.length === 0 && (
                <div className="emptyState">
                  No change orders.
                </div>
              )}

              {changeOrders.map(
                (changeOrder) => (
                  <button
                    type="button"
                    key={changeOrder.id}
                    className={
                      selectedChangeOrder?.id
                        === changeOrder.id
                        ? "commercialListItem active"
                        : "commercialListItem"
                    }
                    onClick={() =>
                      void loadChangeOrder(
                        changeOrder.id
                      )
                    }
                  >
                    <div>
                      <strong>
                        {
                          changeOrder
                            .changeOrderNumber
                        }
                      </strong>

                      <span>
                        {changeOrder.title}
                      </span>
                    </div>

                    <span className="statusBadge">
                      {changeOrder.status}
                    </span>

                    <strong>
                      {money(
                        changeOrder
                          .clientPriceImpact
                      )}
                    </strong>
                  </button>
                )
              )}
            </div>

            {selectedChangeOrder && (
              <article className="changeOrderDocument">
                <header>
                  <div>
                    <span>
                      {
                        selectedChangeOrder
                          .changeOrderNumber
                      }
                    </span>

                    <h3>
                      {
                        selectedChangeOrder
                          .title
                      }
                    </h3>
                  </div>

                  <span className="statusBadge">
                    {
                      selectedChangeOrder
                        .status
                    }
                  </span>
                </header>

                {selectedChangeOrder.description && (
                  <p className="changeOrderDescription">
                    {
                      selectedChangeOrder
                        .description
                    }
                  </p>
                )}

                <div className="changeOrderImpactGrid">
                  {canSeeInternalImpact && (
                    <div>
                      <span>
                        Internal cost impact
                      </span>

                      <strong>
                        {money(
                          selectedChangeOrder
                            .costImpact
                        )}
                      </strong>
                    </div>
                  )}

                  <div>
                    <span>
                      Client price impact
                    </span>

                    <strong>
                      {money(
                        selectedChangeOrder
                          .clientPriceImpact
                      )}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Schedule impact
                    </span>

                    <strong>
                      {
                        selectedChangeOrder
                          .scheduleImpactDays
                      }
                      {" days"}
                    </strong>
                  </div>
                </div>

                {selectedChangeOrder.reason && (
                  <div className="changeOrderReason">
                    <span>
                      Reason
                    </span>

                    <p>
                      {
                        selectedChangeOrder
                          .reason
                      }
                    </p>
                  </div>
                )}

                {canSeeFinancials && (
                  <ChangeOrderImpactPanel
                    changeOrderId={selectedChangeOrder.id}
                    organizationId={organizationId}
                    currency={project.currency}
                    status={selectedChangeOrder.status}
                    version={selectedChangeOrder.updatedAt}
                  />
                )}

                <div className="commercialActions">
                  {selectedChangeOrder.status
                    === "DRAFT"
                    && canManage && (
                    <>
                      <button
                        type="button"
                        onClick={() =>
                          void transition(
                            "submit"
                          )
                        }
                      >
                        Submit
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          void cancelChangeOrder()
                        }
                      >
                        Cancel
                      </button>
                    </>
                  )}

                  {selectedChangeOrder.status
                    === "SUBMITTED"
                    && canManage && (
                    <>
                      <button
                        type="button"
                        onClick={() =>
                          void transition(
                            "client-review"
                          )
                        }
                      >
                        Send to client
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          void cancelChangeOrder()
                        }
                      >
                        Cancel
                      </button>
                    </>
                  )}

                  {selectedChangeOrder.status
                    === "CLIENT_REVIEW"
                    && canDecide && (
                    <>
                      <button
                        type="button"
                        onClick={() =>
                          void transition(
                            "approve"
                          )
                        }
                      >
                        Approve
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          void transition(
                            "reject"
                          )
                        }
                      >
                        Reject
                      </button>
                    </>
                  )}

                  {selectedChangeOrder.status
                    === "CLIENT_REVIEW"
                    && canManage && (
                    <button
                      type="button"
                      onClick={() =>
                        void cancelChangeOrder()
                      }
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </article>
            )}
          </div>

          {canManage && (
            <form
              className="commercialForm"
              onSubmit={createChangeOrder}
            >
              <h3>
                New change request
              </h3>

              <label>
                Title
                <input
                  required
                  maxLength={250}
                  value={title}
                  onChange={(event) =>
                    setTitle(
                      event.target.value
                    )
                  }
                />
              </label>

              <label>
                Description
                <textarea
                  value={description}
                  onChange={(event) =>
                    setDescription(
                      event.target.value
                    )
                  }
                />
              </label>

              <label>
                Reason
                <textarea
                  value={reason}
                  onChange={(event) =>
                    setReason(
                      event.target.value
                    )
                  }
                />
              </label>

              <div className="fieldRow">
                <label>
                  Internal cost impact
                  <input
                    required
                    type="number"
                    step="0.01"
                    value={costImpact}
                    onChange={(event) =>
                      setCostImpact(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Client price impact
                  <input
                    required
                    type="number"
                    step="0.01"
                    value={
                      clientPriceImpact
                    }
                    onChange={(event) =>
                      setClientPriceImpact(
                        event.target.value
                      )
                    }
                  />
                </label>
              </div>

              <label>
                Schedule impact days
                <input
                  required
                  type="number"
                  step="1"
                  value={
                    scheduleImpactDays
                  }
                  onChange={(event) =>
                    setScheduleImpactDays(
                      event.target.value
                    )
                  }
                />
              </label>

              <button type="submit">
                Create change request
              </button>
            </form>
          )}
        </section>
      </section>
    </main>
  );
}
