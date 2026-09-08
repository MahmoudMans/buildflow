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
  FinancialSummary,
  ProjectMember,
  ProjectView,
  PurchaseOrder,
  PurchaseOrderSummary,
  Supplier,
} from "@/lib/types";

export default function ProcurementPage() {
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

  const [summary, setSummary] =
    useState<FinancialSummary | null>(
      null
    );

  const [suppliers, setSuppliers] =
    useState<Supplier[]>([]);

  const [orders, setOrders] =
    useState<PurchaseOrderSummary[]>([]);

  const [members, setMembers] =
    useState<ProjectMember[]>([]);

  const [selectedOrder, setSelectedOrder] =
    useState<PurchaseOrder | null>(
      null
    );

  const [supplierId, setSupplierId] =
    useState("");

  const [issueDate, setIssueDate] =
    useState(
      new Date()
        .toISOString()
        .slice(0, 10)
    );

  const [
    expectedDeliveryDate,
    setExpectedDeliveryDate,
  ] = useState("");

  const [notes, setNotes] =
    useState("");

  const [itemDescription, setItemDescription] =
    useState("");

  const [itemQuantity, setItemQuantity] =
    useState("1");

  const [itemUnit, setItemUnit] =
    useState("unit");

  const [itemUnitPrice, setItemUnitPrice] =
    useState("");

  const [itemTaxRate, setItemTaxRate] =
    useState("0");

  const [
    receiptQuantities,
    setReceiptQuantities,
  ] = useState<
    Record<string, string>
  >({});

  const [receiptDate, setReceiptDate] =
    useState(
      new Date()
        .toISOString()
        .slice(0, 10)
    );

  const [error, setError] =
    useState("");



  async function loadData(
    preferredOrderId?: string
  ) {
    try {
      const [
        projectResult,
        financialResult,
        supplierResult,
        orderResult,
        memberResult,
      ] = await Promise.all([
        apiGet<ProjectView>(
          `/projects/${projectId}`,
          organizationId
        ),

        apiGet<FinancialSummary>(
          `/projects/${projectId}/financial-summary`,
          organizationId
        ),

        apiGet<Supplier[]>(
          "/suppliers",
          organizationId
        ),

        apiGet<PurchaseOrderSummary[]>(
          `/projects/${projectId}/purchase-orders`,
          organizationId
        ),

        apiGet<ProjectMember[]>(
          `/projects/${projectId}/members`,
          organizationId
        ),
      ]);

      setProject(projectResult);
      setSummary(financialResult);
      setSuppliers(
        supplierResult.filter(
          (supplier) =>
            supplier.status === "ACTIVE"
        )
      );
      setOrders(orderResult);
      setMembers(memberResult);

      if (
        !supplierId
        && supplierResult.length > 0
      ) {
        setSupplierId(
          supplierResult[0].id
        );
      }

      const selectedId =
        preferredOrderId
        || selectedOrder?.id
        || orderResult[0]?.id;

      if (selectedId) {
        await loadOrder(
          selectedId
        );
      } else {
        setSelectedOrder(null);
      }
    } catch (caught) {
      handleError(caught);
    }
  }

  async function loadOrder(
    purchaseOrderId: string
  ) {
    try {
      const result =
        await apiGet<PurchaseOrder>(
          `/purchase-orders/${purchaseOrderId}`,
          organizationId
        );

      setSelectedOrder(result);

      const initialQuantities:
        Record<string, string> = {};

      result.items.forEach(
        (item) => {
          initialQuantities[item.id] =
            "";
        }
      );

      setReceiptQuantities(
        initialQuantities
      );
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
    members.find(
      (member) =>
        member.userId === user?.id
    );

  const canManage =
    organization?.role === "OWNER"
    || organization?.role === "ADMIN"
    || currentProjectMember?.projectRole
      === "PROJECT_MANAGER";

  const canApprove =
    organization?.role === "OWNER"
    || organization?.role === "ADMIN";

  const canReceive =
    canManage
    || currentProjectMember?.projectRole
      === "SITE_MANAGER";

  async function createOrder(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      const result =
        await apiPost<PurchaseOrder>(
          `/projects/${projectId}/purchase-orders`,
          {
            supplierId,
            issueDate,
            expectedDeliveryDate:
              expectedDeliveryDate || null,
            notes:
              notes || null,
          },
          organizationId
        );

      setExpectedDeliveryDate("");
      setNotes("");

      await loadData(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function addItem(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!selectedOrder) {
      return;
    }

    try {
      const result =
        await apiPost<PurchaseOrder>(
          `/purchase-orders/${selectedOrder.id}/items`,
          {
            description:
              itemDescription,
            quantity:
              Number(
                itemQuantity
              ),
            unit:
              itemUnit,
            unitPrice:
              Number(
                itemUnitPrice
              ),
            taxRate:
              Number(
                itemTaxRate
              ),
          },
          organizationId
        );

      setSelectedOrder(result);

      setItemDescription("");
      setItemQuantity("1");
      setItemUnit("unit");
      setItemUnitPrice("");
      setItemTaxRate("0");

      await loadData(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function deleteItem(
    itemId: string
  ) {
    if (
      !selectedOrder
      || !window.confirm(
        "Delete this purchase order item?"
      )
    ) {
      return;
    }

    try {
      const result =
        await apiDelete<PurchaseOrder>(
          `/purchase-orders/${selectedOrder.id}/items/${itemId}`,
          organizationId
        );

      setSelectedOrder(result);

      await loadData(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function orderAction(
    path: string,
    body?: unknown
  ) {
    if (!selectedOrder) {
      return;
    }

    try {
      const result =
        await apiPost<PurchaseOrder>(
          `/purchase-orders/${selectedOrder.id}/${path}`,
          body,
          organizationId
        );

      setSelectedOrder(result);

      await loadData(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function cancelOrder() {
    const reason =
      window.prompt(
        "Reason for cancelling this purchase order:"
      );

    if (
      reason === null
      || !reason.trim()
    ) {
      return;
    }

    await orderAction(
      "cancel",
      {
        reason,
      }
    );
  }

  async function recordReceipt(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!selectedOrder) {
      return;
    }

    const items =
      selectedOrder.items
        .map(
          (item) => ({
            purchaseOrderItemId:
              item.id,
            quantityReceived:
              Number(
                receiptQuantities[
                  item.id
                ] || 0
              ),
          })
        )
        .filter(
          (item) =>
            item.quantityReceived > 0
        );

    if (items.length === 0) {
      setError(
        "Enter at least one received quantity."
      );
      return;
    }

    try {
      const result =
        await apiPost<PurchaseOrder>(
          `/purchase-orders/${selectedOrder.id}/receipts`,
          {
            receivedDate:
              receiptDate,
            items,
          },
          organizationId
        );

      setSelectedOrder(result);

      await loadData(
        result.id
      );
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
        : "The procurement operation failed."
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
              Procurement
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="procurement"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="financialMetricGrid">
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
              Outstanding commitments
            </span>

            <strong>
              {money(
                Number(
                  summary.committedCost
                )
              )}
            </strong>
          </article>

          <article>
            <span>
              Forecast cost
            </span>

            <strong>
              {money(
                Number(
                  summary.forecastCost
                )
              )}
            </strong>
          </article>

          <article>
            <span>
              Forecast variance
            </span>

            <strong>
              {money(
                Number(
                  summary.forecastBudgetVariance
                )
              )}
            </strong>
          </article>
        </section>

        <section className="procurementLayout">
          <aside className="purchaseOrderListCard">
            <header>
              <h2>
                Purchase orders
              </h2>

              <span>
                {orders.length}
              </span>
            </header>

            <div>
              {orders.length === 0 && (
                <div className="emptyState">
                  No purchase orders.
                </div>
              )}

              {orders.map(
                (order) => (
                  <button
                    type="button"
                    className={
                      selectedOrder?.id
                        === order.id
                        ? "purchaseOrderListItem active"
                        : "purchaseOrderListItem"
                    }
                    key={order.id}
                    onClick={() =>
                      loadOrder(
                        order.id
                      )
                    }
                  >
                    <div>
                      <strong>
                        {
                          order.purchaseOrderNumber
                        }
                      </strong>

                      <span>
                        {
                          order.supplierName
                        }
                      </span>
                    </div>

                    <span>
                      {order.status}
                    </span>

                    <strong>
                      {money(
                        Number(
                          order.total
                        )
                      )}
                    </strong>
                  </button>
                )
              )}
            </div>
          </aside>

          <section className="purchaseOrderDetail">
            {!selectedOrder && (
              <div className="emptyState procurementEmpty">
                Select or create a purchase order.
              </div>
            )}

            {selectedOrder && (
              <>
                <article className="purchaseOrderHeaderCard">
                  <header>
                    <div>
                      <span>
                        {
                          selectedOrder.purchaseOrderNumber
                        }
                      </span>

                      <h2>
                        {
                          selectedOrder.supplierName
                        }
                      </h2>

                      <p>
                        Issued{" "}
                        {
                          selectedOrder.issueDate
                        }
                      </p>
                    </div>

                    <strong className="statusBadge">
                      {
                        selectedOrder.status
                      }
                    </strong>
                  </header>

                  <div className="poTotals">
                    <div>
                      <span>
                        Subtotal
                      </span>

                      <strong>
                        {money(
                          Number(
                            selectedOrder.subtotal
                          )
                        )}
                      </strong>
                    </div>

                    <div>
                      <span>
                        Tax
                      </span>

                      <strong>
                        {money(
                          Number(
                            selectedOrder.taxAmount
                          )
                        )}
                      </strong>
                    </div>

                    <div>
                      <span>
                        Total
                      </span>

                      <strong>
                        {money(
                          Number(
                            selectedOrder.total
                          )
                        )}
                      </strong>
                    </div>
                  </div>

                  <footer className="poActions">
                    {selectedOrder.status ===
                      "DRAFT"
                      && canManage && (
                      <>
                        <button
                          type="button"
                          onClick={() =>
                            orderAction(
                              "submit"
                            )
                          }
                        >
                          Submit
                        </button>

                        <button
                          type="button"
                          onClick={
                            cancelOrder
                          }
                        >
                          Cancel
                        </button>
                      </>
                    )}

                    {selectedOrder.status ===
                      "PENDING_APPROVAL"
                      && canApprove && (
                      <>
                        <button
                          type="button"
                          onClick={() =>
                            orderAction(
                              "approve"
                            )
                          }
                        >
                          Approve
                        </button>

                        <button
                          type="button"
                          onClick={
                            cancelOrder
                          }
                        >
                          Cancel
                        </button>
                      </>
                    )}

                    {selectedOrder.status ===
                      "APPROVED"
                      && canManage && (
                      <>
                        <button
                          type="button"
                          onClick={() =>
                            orderAction(
                              "order"
                            )
                          }
                        >
                          Mark ordered
                        </button>

                        <button
                          type="button"
                          onClick={
                            cancelOrder
                          }
                        >
                          Cancel
                        </button>
                      </>
                    )}
                  </footer>
                </article>

                <article className="purchaseOrderItemsCard">
                  <header>
                    <h2>
                      Order items
                    </h2>
                  </header>

                  <div className="poItemHeader">
                    <span>
                      Description
                    </span>

                    <span>
                      Qty
                    </span>

                    <span>
                      Received
                    </span>

                    <span>
                      Unit price
                    </span>

                    <span>
                      Tax
                    </span>

                    <span>
                      Total
                    </span>

                    <span />
                  </div>

                  {selectedOrder.items.map(
                    (item) => (
                      <div
                        className="poItemRow"
                        key={item.id}
                      >
                        <strong>
                          {
                            item.description
                          }
                        </strong>

                        <span>
                          {item.quantity}
                          {" "}
                          {item.unit}
                        </span>

                        <span>
                          {
                            item.quantityReceived
                          }
                          {" / "}
                          {item.quantity}
                        </span>

                        <span>
                          {money(
                            Number(
                              item.unitPrice
                            )
                          )}
                        </span>

                        <span>
                          {item.taxRate}%
                        </span>

                        <strong>
                          {money(
                            Number(
                              item.lineTotal
                            )
                          )}
                        </strong>

                        <div>
                          {selectedOrder.status ===
                            "DRAFT"
                            && canManage && (
                            <button
                              type="button"
                              className="tableAction"
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
                </article>

                {selectedOrder.status ===
                  "DRAFT"
                  && canManage && (
                  <article className="moduleFormCard procurementFormCard">
                    <h2>
                      Add order item
                    </h2>

                    <form
                      className="authForm"
                      onSubmit={addItem}
                    >
                      <label>
                        Description
                        <input
                          required
                          value={
                            itemDescription
                          }
                          onChange={(
                            event
                          ) =>
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
                            min="0.0001"
                            step="0.0001"
                            required
                            value={
                              itemQuantity
                            }
                            onChange={(
                              event
                            ) =>
                              setItemQuantity(
                                event.target.value
                              )
                            }
                          />
                        </label>

                        <label>
                          Unit
                          <input
                            required
                            value={
                              itemUnit
                            }
                            onChange={(
                              event
                            ) =>
                              setItemUnit(
                                event.target.value
                              )
                            }
                          />
                        </label>
                      </div>

                      <div className="fieldRow">
                        <label>
                          Unit price
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            required
                            value={
                              itemUnitPrice
                            }
                            onChange={(
                              event
                            ) =>
                              setItemUnitPrice(
                                event.target.value
                              )
                            }
                          />
                        </label>

                        <label>
                          Tax rate %
                          <input
                            type="number"
                            min="0"
                            max="100"
                            step="0.01"
                            required
                            value={
                              itemTaxRate
                            }
                            onChange={(
                              event
                            ) =>
                              setItemTaxRate(
                                event.target.value
                              )
                            }
                          />
                        </label>
                      </div>

                      <button
                        className="authSubmit"
                        type="submit"
                      >
                        Add item
                      </button>
                    </form>
                  </article>
                )}

                {(selectedOrder.status ===
                  "ORDERED"
                  || selectedOrder.status ===
                    "PARTIALLY_RECEIVED")
                  && canReceive && (
                  <article className="moduleFormCard procurementFormCard">
                    <h2>
                      Record delivery
                    </h2>

                    <form
                      className="authForm"
                      onSubmit={
                        recordReceipt
                      }
                    >
                      <label>
                        Received date
                        <input
                          type="date"
                          required
                          value={
                            receiptDate
                          }
                          onChange={(
                            event
                          ) =>
                            setReceiptDate(
                              event.target.value
                            )
                          }
                        />
                      </label>

                      <div className="receiptInputs">
                        {selectedOrder.items
                          .filter(
                            (item) =>
                              Number(
                                item.quantityRemaining
                              ) > 0
                          )
                          .map(
                            (item) => (
                              <label
                                key={
                                  item.id
                                }
                              >
                                <span>
                                  {
                                    item.description
                                  }
                                  {" · remaining "}
                                  {
                                    item.quantityRemaining
                                  }
                                  {" "}
                                  {
                                    item.unit
                                  }
                                </span>

                                <input
                                  type="number"
                                  min="0"
                                  max={
                                    item.quantityRemaining
                                  }
                                  step="0.0001"
                                  value={
                                    receiptQuantities[
                                      item.id
                                    ] || ""
                                  }
                                  onChange={(
                                    event
                                  ) =>
                                    setReceiptQuantities(
                                      (
                                        current
                                      ) => ({
                                        ...current,
                                        [item.id]:
                                          event
                                            .target
                                            .value,
                                      })
                                    )
                                  }
                                />
                              </label>
                            )
                          )}
                      </div>

                      <button
                        className="authSubmit"
                        type="submit"
                      >
                        Record receipt
                      </button>
                    </form>
                  </article>
                )}

                {selectedOrder.receipts.length >
                  0 && (
                  <article className="purchaseOrderItemsCard">
                    <header>
                      <h2>
                        Goods receipts
                      </h2>
                    </header>

                    {selectedOrder.receipts.map(
                      (receipt) => (
                        <div
                          className="goodsReceipt"
                          key={receipt.id}
                        >
                          <div>
                            <strong>
                              {
                                receipt.receivedDate
                              }
                            </strong>

                            <span>
                              {
                                receipt.items.length
                              }
                              {" lines received"}
                            </span>
                          </div>

                          <ul>
                            {receipt.items.map(
                              (item) => (
                                <li
                                  key={
                                    item.purchaseOrderItemId
                                  }
                                >
                                  {
                                    item.description
                                  }
                                  {" · "}
                                  {
                                    item.quantityReceived
                                  }
                                </li>
                              )
                            )}
                          </ul>
                        </div>
                      )
                    )}
                  </article>
                )}
              </>
            )}
          </section>

          {canManage && (
            <aside className="moduleFormCard">
              <h2>
                New purchase order
              </h2>

              <form
                className="authForm"
                onSubmit={createOrder}
              >
                <label>
                  Supplier
                  <select
                    required
                    value={supplierId}
                    onChange={(event) =>
                      setSupplierId(
                        event.target.value
                      )
                    }
                  >
                    <option value="">
                      Select supplier
                    </option>

                    {suppliers.map(
                      (supplier) => (
                        <option
                          key={supplier.id}
                          value={supplier.id}
                        >
                          {supplier.name}
                        </option>
                      )
                    )}
                  </select>
                </label>

                <label>
                  Issue date
                  <input
                    type="date"
                    required
                    value={issueDate}
                    onChange={(event) =>
                      setIssueDate(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Expected delivery
                  <input
                    type="date"
                    value={
                      expectedDeliveryDate
                    }
                    onChange={(event) =>
                      setExpectedDeliveryDate(
                        event.target.value
                      )
                    }
                  />
                </label>

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
                  disabled={!supplierId}
                >
                  Create purchase order
                </button>
              </form>
            </aside>
          )}
        </section>
      </section>
    </main>
  );
}
