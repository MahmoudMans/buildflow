"use client";

import {
  useState,
} from "react";

import {
  downloadApiFile,
} from "@/lib/report-download";

export function ProjectReportActions({
  organizationId,
  projectId,
  projectNumber,
}: {
  organizationId: string;
  projectId: string;
  projectNumber: string;
}) {
  const [busy, setBusy] =
    useState<string | null>(
      null
    );

  const [error, setError] =
    useState("");

  async function download(
    key: string,
    path: string,
    filename: string
  ) {
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
          : "Unable to download report."
      );
    } finally {
      setBusy(null);
    }
  }

  return (
    <section className="projectReportActions">
      <div>
        <span>
          REPORTING
        </span>

        <strong>
          Download project reports
        </strong>
      </div>

      <div className="projectReportButtons">
        <button
          type="button"
          disabled={busy !== null}
          onClick={() =>
            void download(
              "summary",
              `/reports/projects/${projectId}/summary.pdf`,
              `${projectNumber}-summary.pdf`
            )
          }
        >
          {busy === "summary"
            ? "Preparing..."
            : "Project PDF"}
        </button>

        <button
          type="button"
          disabled={busy !== null}
          onClick={() =>
            void download(
              "financial",
              `/reports/projects/${projectId}/financial.pdf`,
              `${projectNumber}-financial.pdf`
            )
          }
        >
          {busy === "financial"
            ? "Preparing..."
            : "Financial PDF"}
        </button>

        <button
          type="button"
          disabled={busy !== null}
          onClick={() =>
            void download(
              "budget",
              `/reports/projects/${projectId}/budget.pdf`,
              `${projectNumber}-budget.pdf`
            )
          }
        >
          {busy === "budget"
            ? "Preparing..."
            : "Budget PDF"}
        </button>

        <button
          type="button"
          disabled={busy !== null}
          onClick={() =>
            void download(
              "expenses",
              `/reports/projects/${projectId}/expenses.pdf`,
              `${projectNumber}-expenses.pdf`
            )
          }
        >
          {busy === "expenses"
            ? "Preparing..."
            : "Expense PDF"}
        </button>
      </div>

      {error && (
        <span className="reportDownloadError">
          {error}
        </span>
      )}
    </section>
  );
}
