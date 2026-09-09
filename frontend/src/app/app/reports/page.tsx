"use client";

import {
  useState,
} from "react";

import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  useAuth,
} from "@/components/AuthProvider";

import {
  useOrganizationSelection,
} from "@/components/useOrganizationSelection";

import {
  downloadApiFile,
} from "@/lib/report-download";

const exports = [
  ["projects", "Projects"],
  ["clients", "Clients"],
  ["expenses", "Expenses"],
  ["invoices", "Invoices"],
  ["payments", "Payments"],
  [
    "purchase-orders",
    "Purchase Orders",
  ],
  ["tasks", "Tasks"],
] as const;

type CommercialType =
  | "quotes"
  | "invoices"
  | "purchase-orders"
  | "change-orders"
  | "site-reports";

export default function ReportsPage() {
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

  const [busy, setBusy] =
    useState<string | null>(
      null
    );

  const [error, setError] =
    useState("");

  const [commercialType, setCommercialType] =
    useState<CommercialType>(
      "invoices"
    );

  const [documentId, setDocumentId] =
    useState("");

  async function download(
    key: string,
    path: string,
    filename: string
  ) {
    if (!organizationId) {
      return;
    }

    try {
      setBusy(key);
      setError("");

      await downloadApiFile(
        path,
        organizationId,
        filename
      );
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Unable to download file."
      );
    } finally {
      setBusy(null);
    }
  }

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
        <header className="appHeader">
          <div>
            <p>
              REPORTING
            </p>

            <h1>
              Reports & Exports
            </h1>

            <span>
              Generate internal PDF
              reports and tenant-scoped
              CSV exports.
            </span>
          </div>
        </header>

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="reportingPanel">
          <header>
            <span>
              CSV
            </span>

            <h2>
              Organization Exports
            </h2>
          </header>

          <div className="reportExportGrid">
            {exports.map(
              ([resource, label]) => (
                <button
                  type="button"
                  key={resource}
                  disabled={busy !== null}
                  onClick={() =>
                    void download(
                      `csv-${resource}`,
                      `/exports/${resource}.csv`,
                      `${resource}.csv`
                    )
                  }
                >
                  {busy ===
                  `csv-${resource}`
                    ? "Exporting..."
                    : `${label} CSV`}
                </button>
              )
            )}
          </div>
        </section>

        <section className="reportingPanel">
          <header>
            <span>
              PDF
            </span>

            <h2>
              Financial Documents
            </h2>
          </header>

          <p className="reportingHelp">
            Enter a document UUID to
            generate its authoritative
            BuildFlow PDF. Project report
            buttons are also available
            directly on each project
            overview.
          </p>

          <div className="reportDocumentForm">
            <label>
              Document type

              <select
                value={commercialType}
                onChange={(event) =>
                  setCommercialType(
                    event.target
                      .value as CommercialType
                  )
                }
              >
                <option value="quotes">
                  Quotation
                </option>

                <option value="invoices">
                  Invoice
                </option>

                <option value="purchase-orders">
                  Purchase Order
                </option>

                <option value="change-orders">
                  Change Order
                </option>

                <option value="site-reports">
                  Daily Site Report
                </option>
              </select>
            </label>

            <label>
              Document UUID

              <input
                value={documentId}
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                onChange={(event) =>
                  setDocumentId(
                    event.target.value
                  )
                }
              />
            </label>

            <button
              type="button"
              disabled={
                busy !== null
                || documentId.trim()
                  .length === 0
              }
              onClick={() =>
                void download(
                  "document-pdf",
                  `/reports/${commercialType}/${documentId.trim()}.pdf`,
                  `${commercialType}-${documentId.trim()}.pdf`
                )
              }
            >
              {busy ===
              "document-pdf"
                ? "Preparing..."
                : "Download PDF"}
            </button>
          </div>
        </section>
      </section>
    </main>
  );
}
