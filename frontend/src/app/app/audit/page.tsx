"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { AuditLogFilters } from "@/components/AuditLogFilters";
import { AuditLogTable } from "@/components/AuditLogTable";
import { AuditPager } from "@/components/AuditPager";
import { OrganizationSelect } from "@/components/OrganizationSelect";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";
import { EmptyState } from "@/components/ux/EmptyState";
import { ErrorState } from "@/components/ux/ErrorState";

import { ApiClientError, apiGet } from "@/lib/api";
import {
  EMPTY_AUDIT_FILTERS,
  buildAuditQuery,
  canViewAuditLog,
  hasActiveFilters,
  validateAuditFilters,
} from "@/lib/audit-log";
import type { AuditFacets, AuditFilters, AuditPage } from "@/lib/audit-types";

// The search, the filters and the results for one organization. The page
// gives it a key, so switching organization starts it afresh.
function AuditPanel({
  organizationId,
  timeZone,
}: {
  organizationId: string;
  timeZone: string;
}) {
  const [filters, setFilters] = useState<AuditFilters>(EMPTY_AUDIT_FILTERS);
  const [pageNumber, setPageNumber] = useState(0);
  const [data, setData] = useState<AuditPage | null>(null);
  const [facets, setFacets] = useState<AuditFacets | null>(null);
  const [facetsFailed, setFacetsFailed] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const requestSequence = useRef(0);

  const filterError = validateAuditFilters(filters);

  const load = useCallback(async () => {
    if (filterError) {
      return;
    }

    const requestId = requestSequence.current + 1;

    requestSequence.current = requestId;

    setBusy(true);
    setError("");

    try {
      const result = await apiGet<AuditPage>(
        `/audit/search${buildAuditQuery(filters, pageNumber)}`,
        organizationId,
      );

      if (requestSequence.current !== requestId) {
        return;
      }

      setData(result);
    } catch (caught) {
      if (requestSequence.current !== requestId) {
        return;
      }

      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load the audit log.",
      );
    } finally {
      if (requestSequence.current === requestId) {
        setBusy(false);
      }
    }
  }, [organizationId, filters, pageNumber, filterError]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load();
    }, 0);

    return () => {
      window.clearTimeout(timer);
    };
  }, [load]);

  useEffect(() => {
    let cancelled = false;

    const timer = window.setTimeout(() => {
      apiGet<AuditFacets>("/audit/facets", organizationId)
        .then((result) => {
          if (!cancelled) {
            setFacets(result);
          }
        })
        .catch(() => {
          if (!cancelled) {
            setFacetsFailed(true);
          }
        });
    }, 0);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [organizationId]);

  function applyFilters(next: AuditFilters) {
    setFilters(next);
    setPageNumber(0);
  }

  return (
    <>
      <AuditLogFilters
        filters={filters}
        facets={facets}
        error={filterError}
        onChange={applyFilters}
      />

      {facetsFailed && (
        <p className="uxAuditNote">
          The lists of projects and people could not be loaded. You can still
          search by text and date.
        </p>
      )}

      <p className="uxAuditNote">
        Times are shown in the organization&apos;s time zone ({timeZone}).
      </p>

      {error && (
        <ErrorState
          title="Could not load the audit log"
          message={error}
          onRetry={() => {
            void load();
          }}
        />
      )}

      {!data && !error && !filterError && (
        <p className="uxAuditNote" role="status">
          Loading the audit log...
        </p>
      )}

      {!error && data && data.totalElements === 0 && (
        <EmptyState
          title={
            hasActiveFilters(filters)
              ? "No entries match these filters"
              : "Nothing has been recorded yet"
          }
          description={
            hasActiveFilters(filters)
              ? "Try a wider date range or remove a filter."
              : "Actions taken in this organization will appear here."
          }
          action={
            hasActiveFilters(filters) ? (
              <button
                type="button"
                className="uxSecondaryButton"
                onClick={() => applyFilters(EMPTY_AUDIT_FILTERS)}
              >
                Clear filters
              </button>
            ) : undefined
          }
        />
      )}

      {!error && data && data.totalElements > 0 && (
        <div
          className={busy ? "uxAuditResults uxAuditBusy" : "uxAuditResults"}
          aria-busy={busy}
        >
          <AuditLogTable items={data.items} timeZone={timeZone} />

          <AuditPager
            page={data}
            busy={busy}
            onPage={(next) => setPageNumber(next)}
          />
        </div>
      )}
    </>
  );
}

export default function AuditPage() {
  const router = useRouter();

  const { user, organizations, loading } = useAuth();

  const organizationSelection = useOrganizationSelection(organizations);
  const organizationId = organizationSelection.organizationId;
  const organization = organizations.find((item) => item.id === organizationId);

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, user, router]);

  if (loading || !user) {
    return <main className="appLoading">Loading...</main>;
  }

  return (
    <main className="appShell">
      <AppSidebar active="audit" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>{organization?.name ?? "Organization"}</p>

            <h1>Audit log</h1>

            <span className="projectClientName">Who did what, and when</span>
          </div>

          {organizations.length > 1 && (
            <label className="uxAuditOrg">
              <span>Organization</span>

              <OrganizationSelect />
            </label>
          )}
        </header>

        {!organization && (
          <EmptyState
            title="No organization selected"
            description="Choose an organization to see its audit log."
          />
        )}

        {organization && !canViewAuditLog(organization.role) && (
          <EmptyState
            title="The audit log is for owners and administrators"
            description={`Ask an owner or administrator of ${organization.name} if you need to see what has been changed.`}
          />
        )}

        {organization && canViewAuditLog(organization.role) && (
          <AuditPanel
            key={organization.id}
            organizationId={organization.id}
            timeZone={organization.timezone}
          />
        )}
      </section>
    </main>
  );
}
