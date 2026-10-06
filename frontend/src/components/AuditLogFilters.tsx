"use client";

import { useId, useState } from "react";
import type { FormEvent } from "react";

import {
  AUDIT_TEXT_MAX,
  EMPTY_AUDIT_FILTERS,
  activeFilterChips,
  actorOptionLabel,
  hasActiveFilters,
  humanizeAction,
  humanizeEntityType,
  projectOptionLabel,
} from "@/lib/audit-log";
import type { AuditFacets, AuditFilters } from "@/lib/audit-types";

type Props = {
  filters: AuditFilters;
  // null until the lists of choices have loaded.
  facets: AuditFacets | null;
  // A message about the chosen filters, such as a start date after the end.
  error?: string | null;
  onChange: (next: AuditFilters) => void;
};

export function AuditLogFilters({ filters, facets, error, onChange }: Props) {
  const id = useId();

  // What is typed in the search box. It is applied when the person presses
  // Search or changes another filter, not on every key.
  const [draft, setDraft] = useState(filters.q);
  const [seenQuery, setSeenQuery] = useState(filters.q);

  // If the search text is changed from outside (a chip or "Clear"), follow it.
  if (filters.q !== seenQuery) {
    setSeenQuery(filters.q);
    setDraft(filters.q);
  }

  function change(patch: Partial<AuditFilters>) {
    onChange({ ...filters, q: draft.trim(), ...patch });
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    change({});
  }

  function clearAll() {
    setDraft("");
    onChange(EMPTY_AUDIT_FILTERS);
  }

  const chips = activeFilterChips(filters, facets);

  return (
    <form
      className="uxAuditFilters"
      role="search"
      aria-label="Filter the audit log"
      onSubmit={submit}
    >
      <div className="uxAuditSearchRow">
        <div className="uxAuditField grow">
          <label htmlFor={`${id}-q`}>Search</label>

          <input
            id={`${id}-q`}
            type="search"
            value={draft}
            maxLength={AUDIT_TEXT_MAX}
            placeholder="A person, an action, or words in the record"
            onChange={(event) => setDraft(event.target.value)}
          />
        </div>

        <button type="submit" className="uxPrimaryButton">
          Search
        </button>
      </div>

      <div className="uxAuditChoices">
        <div className="uxAuditField">
          <label htmlFor={`${id}-project`}>Project</label>

          <select
            id={`${id}-project`}
            value={filters.projectId}
            disabled={!facets}
            onChange={(event) => change({ projectId: event.target.value })}
          >
            <option value="">All projects</option>

            {facets?.projects.map((project) => (
              <option key={project.projectId} value={project.projectId}>
                {projectOptionLabel(project)}
              </option>
            ))}
          </select>
        </div>

        <div className="uxAuditField">
          <label htmlFor={`${id}-person`}>Person</label>

          <select
            id={`${id}-person`}
            value={filters.actorUserId}
            disabled={!facets}
            onChange={(event) => change({ actorUserId: event.target.value })}
          >
            <option value="">Everyone</option>

            {facets?.actors.map((actor) => (
              <option key={actor.userId} value={actor.userId}>
                {actorOptionLabel(actor)}
              </option>
            ))}
          </select>
        </div>

        <div className="uxAuditField">
          <label htmlFor={`${id}-kind`}>Kind of record</label>

          <select
            id={`${id}-kind`}
            value={filters.entityType}
            disabled={!facets}
            onChange={(event) => change({ entityType: event.target.value })}
          >
            <option value="">All kinds</option>

            {facets?.entityTypes.map((entityType) => (
              <option key={entityType} value={entityType}>
                {humanizeEntityType(entityType)}
              </option>
            ))}
          </select>
        </div>

        <div className="uxAuditField">
          <label htmlFor={`${id}-action`}>Action</label>

          <select
            id={`${id}-action`}
            value={filters.action}
            disabled={!facets}
            onChange={(event) => change({ action: event.target.value })}
          >
            <option value="">All actions</option>

            {facets?.actions.map((action) => (
              <option key={action} value={action}>
                {humanizeAction(action)}
              </option>
            ))}
          </select>
        </div>

        <div className="uxAuditField">
          <label htmlFor={`${id}-from`}>From</label>

          <input
            id={`${id}-from`}
            type="date"
            value={filters.from}
            max={filters.to || undefined}
            onChange={(event) => change({ from: event.target.value })}
          />
        </div>

        <div className="uxAuditField">
          <label htmlFor={`${id}-to`}>To</label>

          <input
            id={`${id}-to`}
            type="date"
            value={filters.to}
            min={filters.from || undefined}
            onChange={(event) => change({ to: event.target.value })}
          />
        </div>
      </div>

      {error && (
        <p className="uxFieldError" role="alert">
          {error}
        </p>
      )}

      {chips.length > 0 && (
        <div className="uxAuditActive">
          <ul className="uxAuditChips" aria-label="Active filters">
            {chips.map((chip) => (
              <li key={chip.key}>
                <button
                  type="button"
                  aria-label={`Remove filter ${chip.label}`}
                  onClick={() =>
                    onChange({
                      ...filters,
                      q: chip.key === "q" ? "" : draft.trim(),
                      [chip.key]: "",
                    })
                  }
                >
                  <span>{chip.label}</span>
                  <span aria-hidden="true">×</span>
                </button>
              </li>
            ))}
          </ul>

          {hasActiveFilters(filters) && (
            <button type="button" className="uxLinkButton" onClick={clearAll}>
              Clear all
            </button>
          )}
        </div>
      )}
    </form>
  );
}
