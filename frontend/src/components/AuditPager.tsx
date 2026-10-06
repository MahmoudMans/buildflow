"use client";

import { rangeLabel } from "@/lib/audit-log";
import type { AuditPage } from "@/lib/audit-types";

export function AuditPager({
  page,
  busy,
  onPage,
}: {
  page: AuditPage;
  busy: boolean;
  onPage: (next: number) => void;
}) {
  const hasPages = page.totalPages > 1;

  return (
    <nav className="uxAuditPager" aria-label="Audit log pages">
      <span role="status">{rangeLabel(page)}</span>

      {hasPages && (
        <div className="uxAuditPagerButtons">
          <button
            type="button"
            className="uxSecondaryButton"
            disabled={busy || page.page <= 0}
            onClick={() => onPage(Math.max(0, Math.min(page.page - 1, page.totalPages - 1)))}
          >
            Previous
          </button>

          <span>
            Page {Math.min(page.page + 1, page.totalPages)} of {page.totalPages}
          </span>

          <button
            type="button"
            className="uxSecondaryButton"
            disabled={busy || page.page + 1 >= page.totalPages}
            onClick={() => onPage(page.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </nav>
  );
}
