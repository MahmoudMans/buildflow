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
  Invoice,
  InvoiceSummary,
  PaymentMethod,
  Quote,
  QuoteSummary,
  RevenueSummary,
} from "@/lib/commercial-types";

export default function CommercialPage() {
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

  const [revenue, setRevenue] =
    useState<RevenueSummary | null>(
      null
    );

  const [quotes, setQuotes] =
    useState<QuoteSummary[]>([]);

  const [invoices, setInvoices] =
    useState<InvoiceSummary[]>([]);

  const [
    selectedQuote,
    setSelectedQuote,
  ] =
    useState<Quote | null>(
      null
    );

  const [
    selectedInvoice,
    setSelectedInvoice,
  ] =
    useState<Invoice | null>(
      null
    );

  const [quoteIssueDate, setQuoteIssueDate] =
    useState(
      new Date()
        .toISOString()
        .slice(0, 10)
    );

  const [quoteValidUntil, setQuoteValidUntil] =
    useState("");

  const [quoteNotes, setQuoteNotes] =
    useState("");

  const [quoteTerms, setQuoteTerms] =
    useState("");

  const [contractQuote, setContractQuote] =
    useState(false);

  const [lineDescription, setLineDescription] =
    useState("");

  const [lineQuantity, setLineQuantity] =
    useState("1");

  const [lineUnit, setLineUnit] =
    useState("unit");

  const [lineUnitPrice, setLineUnitPrice] =
    useState("");

  const [lineTaxRate, setLineTaxRate] =
    useState("0");

  const [paymentAmount, setPaymentAmount] =
    useState("");

  const [paymentMethod, setPaymentMethod] =
    useState<PaymentMethod>(
      "BANK_TRANSFER"
    );

  const [paymentReference, setPaymentReference] =
    useState("");

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
            : "The commercial operation failed."
        );
      },
      []
    );

  const loadQuote =
    useCallback(
      async (
        quoteId: string
      ) => {
        if (!organizationId) {
          return;
        }

        try {
          const result =
            await apiGet<Quote>(
              `/quotes/${quoteId}`,
              organizationId
            );

          setSelectedQuote(
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

  const loadInvoice =
    useCallback(
      async (
        invoiceId: string
      ) => {
        if (!organizationId) {
          return;
        }

        try {
          const result =
            await apiGet<Invoice>(
              `/invoices/${invoiceId}`,
              organizationId
            );

          setSelectedInvoice(
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
        preferredQuoteId?: string,
        preferredInvoiceId?: string
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
            revenueResult,
            quoteResult,
            invoiceResult,
          ] =
            await Promise.all([
              apiGet<ProjectView>(
                `/projects/${projectId}`,
                organizationId
              ),

              apiGet<RevenueSummary>(
                `/projects/${projectId}/commercial-summary`,
                organizationId
              ),

              apiGet<QuoteSummary[]>(
                `/projects/${projectId}/quotes`,
                organizationId
              ),

              apiGet<InvoiceSummary[]>(
                `/projects/${projectId}/invoices`,
                organizationId
              ),
            ]);

          setProject(
            projectResult
          );

          setRevenue(
            revenueResult
          );

          setQuotes(
            quoteResult
          );

          setInvoices(
            invoiceResult
          );

          if (
            quoteResult.length === 0
          ) {
            setSelectedQuote(
              null
            );
          } else {
            const currentQuoteId =
              preferredQuoteId;

            const nextQuoteId =
              currentQuoteId
              && quoteResult.some(
                (quote) =>
                  quote.id
                    === currentQuoteId
              )
                ? currentQuoteId
                : quoteResult[0].id;

            await loadQuote(
              nextQuoteId
            );
          }

          if (
            invoiceResult.length === 0
          ) {
            setSelectedInvoice(
              null
            );
          } else {
            const currentInvoiceId =
              preferredInvoiceId;

            const nextInvoiceId =
              currentInvoiceId
              && invoiceResult.some(
                (invoice) =>
                  invoice.id
                    === currentInvoiceId
              )
                ? currentInvoiceId
                : invoiceResult[0].id;

            await loadInvoice(
              nextInvoiceId
            );
          }
        } catch (caught) {
          handleError(caught);
        }
      },
      [
        organizationId,
        projectId,
        loadQuote,
        loadInvoice,
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

  const organization =
    organizations.find(
      (entry) =>
        entry.id
          === organizationId
    );

  const canManage =
    organization?.role === "OWNER"
    || organization?.role === "ADMIN"
    || organization?.role === "ACCOUNTANT";

  const canDecide =
    organization?.role === "OWNER"
    || organization?.role === "ADMIN"
    || organization?.role === "CLIENT";

  async function createQuote(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      const result =
        await apiPost<Quote>(
          `/projects/${projectId}/quotes`,
          {
            issueDate:
              quoteIssueDate,
            validUntil:
              quoteValidUntil || null,
            notes:
              quoteNotes || null,
            terms:
              quoteTerms || null,
            contractQuote,
          },
          organizationId
        );

      setQuoteValidUntil("");
      setQuoteNotes("");
      setQuoteTerms("");
      setContractQuote(false);

      await loadData();
      await loadQuote(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function addQuoteLine(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!selectedQuote) {
      return;
    }

    try {
      const result =
        await apiPost<Quote>(
          `/quotes/${selectedQuote.id}/items`,
          {
            description:
              lineDescription,
            quantity:
              Number(lineQuantity),
            unit:
              lineUnit,
            unitPrice:
              Number(lineUnitPrice),
            taxRate:
              Number(lineTaxRate),
          },
          organizationId
        );

      setSelectedQuote(
        result
      );

      setLineDescription("");
      setLineQuantity("1");
      setLineUnit("unit");
      setLineUnitPrice("");
      setLineTaxRate("0");

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function quoteAction(
    action: string
  ) {
    if (!selectedQuote) {
      return;
    }

    try {
      const result =
        await apiPost<Quote>(
          `/quotes/${selectedQuote.id}/${action}`,
          undefined,
          organizationId
        );

      setSelectedQuote(
        result
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function cancelQuote() {
    if (!selectedQuote) {
      return;
    }

    const reason =
      window.prompt(
        "Reason for cancelling this quotation:"
      );

    if (
      reason === null
      || !reason.trim()
    ) {
      return;
    }

    try {
      const result =
        await apiPost<Quote>(
          `/quotes/${selectedQuote.id}/cancel`,
          {
            reason,
          },
          organizationId
        );

      setSelectedQuote(
        result
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function createInvoiceFromQuote() {
    if (!selectedQuote) {
      return;
    }

    const dueDate =
      window.prompt(
        "Invoice due date (YYYY-MM-DD):"
      );

    if (
      dueDate === null
      || !dueDate.trim()
    ) {
      return;
    }

    try {
      const result =
        await apiPost<Invoice>(
          `/quotes/${selectedQuote.id}/create-invoice`,
          {
            issueDate:
              new Date()
                .toISOString()
                .slice(0, 10),
            dueDate,
            notes:
              `Created from ${selectedQuote.quoteNumber}`,
            paymentInstructions:
              "Bank transfer",
          },
          organizationId
        );

      await loadData();

      await loadInvoice(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function invoiceAction(
    action: string
  ) {
    if (!selectedInvoice) {
      return;
    }

    try {
      const result =
        await apiPost<Invoice>(
          `/invoices/${selectedInvoice.id}/${action}`,
          undefined,
          organizationId
        );

      setSelectedInvoice(
        result
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function voidInvoice() {
    if (!selectedInvoice) {
      return;
    }

    const reason =
      window.prompt(
        "Reason for voiding this invoice:"
      );

    if (
      reason === null
      || !reason.trim()
    ) {
      return;
    }

    try {
      const result =
        await apiPost<Invoice>(
          `/invoices/${selectedInvoice.id}/void`,
          {
            reason,
          },
          organizationId
        );

      setSelectedInvoice(
        result
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function recordPayment(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!selectedInvoice) {
      return;
    }

    try {
      const result =
        await apiPost<Invoice>(
          `/invoices/${selectedInvoice.id}/payments`,
          {
            paymentDate:
              new Date()
                .toISOString()
                .slice(0, 10),
            amount:
              Number(
                paymentAmount
              ),
            method:
              paymentMethod,
            reference:
              paymentReference || null,
            notes:
              null,
          },
          organizationId
        );

      setSelectedInvoice(
        result
      );

      setPaymentAmount("");
      setPaymentReference("");

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  function money(
    value: number
  ) {
    return new Intl.NumberFormat(
      "en-US",
      {
        style: "currency",
        currency:
          project?.currency
          ?? "EUR",
      }
    ).format(
      Number(value)
    );
  }

  if (
    loading
    || !user
    || !project
    || !revenue
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
              Client commercial
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="commercial"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="commercialMetrics">
          <article>
            <span>
              Contract value
            </span>

            <strong>
              {money(
                revenue.originalContractValue
              )}
            </strong>
          </article>

          <article>
            <span>
              Total invoiced
            </span>

            <strong>
              {money(
                revenue.totalInvoiced
              )}
            </strong>
          </article>

          <article>
            <span>
              Total paid
            </span>

            <strong>
              {money(
                revenue.totalPaid
              )}
            </strong>
          </article>

          <article>
            <span>
              Receivables
            </span>

            <strong>
              {money(
                revenue.outstandingReceivables
              )}
            </strong>
          </article>

          <article>
            <span>
              Forecast profit
            </span>

            <strong>
              {money(
                revenue.forecastProfit
              )}
            </strong>
          </article>

          <article>
            <span>
              Forecast margin
            </span>

            <strong>
              {Number(
                revenue.forecastMarginPercentage
              ).toFixed(1)}
              %
            </strong>
          </article>
        </section>

        <section className="commercialGrid">
          <div className="commercialColumn">
            <div className="commercialSectionHeader">
              <div>
                <p>
                  CLIENT PRICING
                </p>

                <h2>
                  Quotations
                </h2>
              </div>

              <span>
                {quotes.length}
              </span>
            </div>

            <div className="commercialList">
              {quotes.length === 0 && (
                <div className="emptyState">
                  No quotations.
                </div>
              )}

              {quotes.map(
                (quote) => (
                  <button
                    type="button"
                    key={quote.id}
                    className={
                      selectedQuote?.id
                        === quote.id
                        ? "commercialListItem active"
                        : "commercialListItem"
                    }
                    onClick={() =>
                      void loadQuote(
                        quote.id
                      )
                    }
                  >
                    <div>
                      <strong>
                        {quote.quoteNumber}
                      </strong>

                      <span>
                        {quote.contractQuote
                          ? "Contract quote"
                          : "Quotation"}
                      </span>
                    </div>

                    <span className="statusBadge">
                      {quote.status}
                    </span>

                    <strong>
                      {money(
                        quote.total
                      )}
                    </strong>
                  </button>
                )
              )}
            </div>

            {selectedQuote && (
              <article className="commercialDocument">
                <header>
                  <div>
                    <span>
                      {
                        selectedQuote.quoteNumber
                      }
                    </span>

                    <h3>
                      Quotation
                    </h3>
                  </div>

                  <span className="statusBadge">
                    {selectedQuote.status}
                  </span>
                </header>

                <div className="commercialDocumentTotals">
                  <div>
                    <span>
                      Subtotal
                    </span>

                    <strong>
                      {money(
                        selectedQuote.subtotal
                      )}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Tax
                    </span>

                    <strong>
                      {money(
                        selectedQuote.taxAmount
                      )}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Total
                    </span>

                    <strong>
                      {money(
                        selectedQuote.total
                      )}
                    </strong>
                  </div>
                </div>

                <div className="commercialLines">
                  {selectedQuote.items.map(
                    (item) => (
                      <div
                        key={item.id}
                        className="commercialLine"
                      >
                        <div>
                          <strong>
                            {item.description}
                          </strong>

                          <span>
                            {item.quantity}
                            {" "}
                            {item.unit}
                            {" × "}
                            {money(
                              item.unitPrice
                            )}
                          </span>
                        </div>

                        <strong>
                          {money(
                            item.lineTotal
                          )}
                        </strong>
                      </div>
                    )
                  )}
                </div>

                <div className="commercialActions">
                  {selectedQuote.status
                    === "DRAFT"
                    && canManage && (
                    <>
                      <button
                        type="button"
                        onClick={() =>
                          void quoteAction(
                            "send"
                          )
                        }
                      >
                        Send quote
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          void cancelQuote()
                        }
                      >
                        Cancel
                      </button>
                    </>
                  )}

                  {(selectedQuote.status
                    === "SENT"
                    || selectedQuote.status
                      === "VIEWED")
                    && canDecide && (
                    <>
                      <button
                        type="button"
                        onClick={() =>
                          void quoteAction(
                            "accept"
                          )
                        }
                      >
                        Accept
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          void quoteAction(
                            "reject"
                          )
                        }
                      >
                        Reject
                      </button>
                    </>
                  )}

                  {selectedQuote.status
                    === "ACCEPTED"
                    && canManage && (
                    <button
                      type="button"
                      onClick={() =>
                        void createInvoiceFromQuote()
                      }
                    >
                      Create invoice
                    </button>
                  )}
                </div>
              </article>
            )}

            {selectedQuote?.status === "DRAFT"
              && canManage && (
              <form
                className="commercialForm"
                onSubmit={addQuoteLine}
              >
                <h3>
                  Add quote line
                </h3>

                <label>
                  Description
                  <input
                    required
                    value={lineDescription}
                    onChange={(event) =>
                      setLineDescription(
                        event.target.value
                      )
                    }
                  />
                </label>

                <div className="fieldRow">
                  <label>
                    Quantity
                    <input
                      required
                      type="number"
                      min="0.0001"
                      step="0.0001"
                      value={lineQuantity}
                      onChange={(event) =>
                        setLineQuantity(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <label>
                    Unit
                    <input
                      required
                      value={lineUnit}
                      onChange={(event) =>
                        setLineUnit(
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
                      required
                      type="number"
                      min="0"
                      step="0.01"
                      value={lineUnitPrice}
                      onChange={(event) =>
                        setLineUnitPrice(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <label>
                    Tax %
                    <input
                      required
                      type="number"
                      min="0"
                      max="100"
                      step="0.01"
                      value={lineTaxRate}
                      onChange={(event) =>
                        setLineTaxRate(
                          event.target.value
                        )
                      }
                    />
                  </label>
                </div>

                <button type="submit">
                  Add line
                </button>
              </form>
            )}

            {canManage && (
              <form
                className="commercialForm"
                onSubmit={createQuote}
              >
                <h3>
                  New quotation
                </h3>

                <label>
                  Issue date
                  <input
                    required
                    type="date"
                    value={quoteIssueDate}
                    onChange={(event) =>
                      setQuoteIssueDate(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Valid until
                  <input
                    type="date"
                    value={quoteValidUntil}
                    onChange={(event) =>
                      setQuoteValidUntil(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Notes
                  <textarea
                    value={quoteNotes}
                    onChange={(event) =>
                      setQuoteNotes(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Terms
                  <textarea
                    value={quoteTerms}
                    onChange={(event) =>
                      setQuoteTerms(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label className="commercialCheckbox">
                  <input
                    type="checkbox"
                    checked={contractQuote}
                    onChange={(event) =>
                      setContractQuote(
                        event.target.checked
                      )
                    }
                  />

                  Establish project contract value when accepted
                </label>

                <button type="submit">
                  Create quotation
                </button>
              </form>
            )}
          </div>

          <div className="commercialColumn">
            <div className="commercialSectionHeader">
              <div>
                <p>
                  RECEIVABLES
                </p>

                <h2>
                  Invoices
                </h2>
              </div>

              <span>
                {invoices.length}
              </span>
            </div>

            <div className="commercialList">
              {invoices.length === 0 && (
                <div className="emptyState">
                  No invoices.
                </div>
              )}

              {invoices.map(
                (invoice) => (
                  <button
                    type="button"
                    key={invoice.id}
                    className={
                      selectedInvoice?.id
                        === invoice.id
                        ? "commercialListItem active"
                        : "commercialListItem"
                    }
                    onClick={() =>
                      void loadInvoice(
                        invoice.id
                      )
                    }
                  >
                    <div>
                      <strong>
                        {invoice.invoiceNumber}
                      </strong>

                      <span>
                        Due {invoice.dueDate}
                      </span>
                    </div>

                    <span className="statusBadge">
                      {invoice.status}
                    </span>

                    <strong>
                      {money(
                        invoice.amountOutstanding
                      )}
                    </strong>
                  </button>
                )
              )}
            </div>

            {selectedInvoice && (
              <article className="commercialDocument">
                <header>
                  <div>
                    <span>
                      {
                        selectedInvoice.invoiceNumber
                      }
                    </span>

                    <h3>
                      Invoice
                    </h3>
                  </div>

                  <span className="statusBadge">
                    {selectedInvoice.status}
                  </span>
                </header>

                <div className="commercialDocumentTotals">
                  <div>
                    <span>
                      Total
                    </span>

                    <strong>
                      {money(
                        selectedInvoice.total
                      )}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Paid
                    </span>

                    <strong>
                      {money(
                        selectedInvoice.amountPaid
                      )}
                    </strong>
                  </div>

                  <div>
                    <span>
                      Outstanding
                    </span>

                    <strong>
                      {money(
                        selectedInvoice.amountOutstanding
                      )}
                    </strong>
                  </div>
                </div>

                <div className="commercialLines">
                  {selectedInvoice.items.map(
                    (item) => (
                      <div
                        key={item.id}
                        className="commercialLine"
                      >
                        <div>
                          <strong>
                            {item.description}
                          </strong>

                          <span>
                            {item.quantity}
                            {" "}
                            {item.unit}
                          </span>
                        </div>

                        <strong>
                          {money(
                            item.lineTotal
                          )}
                        </strong>
                      </div>
                    )
                  )}
                </div>

                <div className="commercialActions">
                  {selectedInvoice.status
                    === "DRAFT"
                    && canManage && (
                    <button
                      type="button"
                      onClick={() =>
                        void invoiceAction(
                          "issue"
                        )
                      }
                    >
                      Issue invoice
                    </button>
                  )}

                  {(selectedInvoice.status
                    === "ISSUED"
                    || selectedInvoice.status
                      === "OVERDUE")
                    && Number(
                      selectedInvoice.amountPaid
                    ) === 0
                    && canManage && (
                    <button
                      type="button"
                      onClick={() =>
                        void voidInvoice()
                      }
                    >
                      Void invoice
                    </button>
                  )}
                </div>

                {selectedInvoice.payments.length > 0 && (
                  <div className="commercialPayments">
                    <h4>
                      Payments
                    </h4>

                    {selectedInvoice.payments.map(
                      (payment) => (
                        <div
                          key={payment.id}
                          className="commercialPayment"
                        >
                          <div>
                            <strong>
                              {payment.method}
                            </strong>

                            <span>
                              {payment.paymentDate}
                            </span>
                          </div>

                          <strong>
                            {money(
                              payment.amount
                            )}
                          </strong>
                        </div>
                      )
                    )}
                  </div>
                )}
              </article>
            )}

            {selectedInvoice
              && selectedInvoice.status !== "DRAFT"
              && selectedInvoice.status !== "PAID"
              && selectedInvoice.status !== "VOID"
              && canManage && (
              <form
                className="commercialForm"
                onSubmit={recordPayment}
              >
                <h3>
                  Record payment
                </h3>

                <label>
                  Amount
                  <input
                    required
                    type="number"
                    min="0.01"
                    step="0.01"
                    max={
                      selectedInvoice.amountOutstanding
                    }
                    value={paymentAmount}
                    onChange={(event) =>
                      setPaymentAmount(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Method
                  <select
                    value={paymentMethod}
                    onChange={(event) =>
                      setPaymentMethod(
                        event.target.value as PaymentMethod
                      )
                    }
                  >
                    <option value="BANK_TRANSFER">
                      Bank transfer
                    </option>

                    <option value="CASH">
                      Cash
                    </option>

                    <option value="CARD">
                      Card
                    </option>

                    <option value="CHEQUE">
                      Cheque
                    </option>

                    <option value="OTHER">
                      Other
                    </option>
                  </select>
                </label>

                <label>
                  Reference
                  <input
                    value={paymentReference}
                    onChange={(event) =>
                      setPaymentReference(
                        event.target.value
                      )
                    }
                  />
                </label>

                <button type="submit">
                  Record payment
                </button>
              </form>
            )}
          </div>
        </section>
      </section>
    </main>
  );
}
