import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { EMPTY_AUDIT_FILTERS } from "@/lib/audit-log";
import type { AuditFacets, AuditFilters } from "@/lib/audit-types";

import { AuditLogFilters } from "./AuditLogFilters";

const PROJECT = "22222222-0000-0000-0000-000000000001";
const PERSON = "11111111-0000-0000-0000-00000000000a";

const facets: AuditFacets = {
  actions: ["TASK_CREATED", "TASK_UPDATED"],
  entityTypes: ["SITE_REPORT", "TASK"],
  actors: [{ userId: PERSON, name: "Ann Builder", email: "ann@acme.test" }],
  projects: [{ projectId: PROJECT, name: "Riverside Lofts", projectNumber: "PRJ-0001" }],
};

function filters(change: Partial<AuditFilters> = {}): AuditFilters {
  return { ...EMPTY_AUDIT_FILTERS, ...change };
}

afterEach(() => {
  cleanup();
});

describe("AuditLogFilters", () => {
  it("offers the projects, people, kinds and actions that exist", () => {
    render(<AuditLogFilters filters={filters()} facets={facets} onChange={vi.fn()} />);

    expect(screen.getByRole("option", { name: "PRJ-0001 · Riverside Lofts" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Ann Builder (ann@acme.test)" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Site report" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Task updated" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "All projects" })).toBeTruthy();
  });

  it("keeps the lists switched off until they have loaded", () => {
    render(<AuditLogFilters filters={filters()} facets={null} onChange={vi.fn()} />);

    for (const name of ["Project", "Person", "Kind of record", "Action"]) {
      expect(screen.getByRole("combobox", { name }).hasAttribute("disabled")).toBe(true);
    }

    expect(screen.getByRole("searchbox", { name: "Search" }).hasAttribute("disabled")).toBe(false);
  });

  it("applies a choice at once, together with the text already typed", () => {
    const onChange = vi.fn();

    render(<AuditLogFilters filters={filters()} facets={facets} onChange={onChange} />);

    fireEvent.change(screen.getByRole("searchbox", { name: "Search" }), {
      target: { value: "  slab " },
    });

    fireEvent.change(screen.getByRole("combobox", { name: "Project" }), {
      target: { value: PROJECT },
    });

    expect(onChange).toHaveBeenCalledWith(filters({ q: "slab", projectId: PROJECT }));
  });

  it("applies the search text when Search is pressed", () => {
    const onChange = vi.fn();

    render(<AuditLogFilters filters={filters()} facets={facets} onChange={onChange} />);

    fireEvent.change(screen.getByRole("searchbox", { name: "Search" }), {
      target: { value: " pour slab " },
    });

    expect(onChange).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Search" }));

    expect(onChange).toHaveBeenCalledWith(filters({ q: "pour slab" }));
  });

  it("applies the dates", () => {
    const onChange = vi.fn();

    render(<AuditLogFilters filters={filters()} facets={facets} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText("From"), { target: { value: "2026-03-01" } });

    expect(onChange).toHaveBeenCalledWith(filters({ from: "2026-03-01" }));
  });

  it("shows each filter that is on, and removes one when asked", () => {
    const onChange = vi.fn();

    render(
      <AuditLogFilters
        filters={filters({ q: "slab", projectId: PROJECT, from: "2026-03-01" })}
        facets={facets}
        onChange={onChange}
      />,
    );

    expect(screen.getByRole("button", { name: "Remove filter Search: “slab”" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Remove filter From: 01 Mar 2026" })).toBeTruthy();

    fireEvent.click(
      screen.getByRole("button", { name: "Remove filter Project: PRJ-0001 · Riverside Lofts" }),
    );

    expect(onChange).toHaveBeenCalledWith(filters({ q: "slab", from: "2026-03-01" }));
  });

  it("clears everything", () => {
    const onChange = vi.fn();

    render(
      <AuditLogFilters
        filters={filters({ q: "slab", projectId: PROJECT })}
        facets={facets}
        onChange={onChange}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Clear all" }));

    expect(onChange).toHaveBeenCalledWith(EMPTY_AUDIT_FILTERS);
  });

  it("has no chips or Clear all when nothing is chosen", () => {
    render(<AuditLogFilters filters={filters()} facets={facets} onChange={vi.fn()} />);

    expect(screen.queryByRole("button", { name: "Clear all" })).toBeNull();
    expect(screen.queryByRole("list", { name: "Active filters" })).toBeNull();
  });

  it("follows the search text when it is changed from outside", () => {
    const { rerender } = render(
      <AuditLogFilters filters={filters({ q: "slab" })} facets={facets} onChange={vi.fn()} />,
    );

    expect((screen.getByRole("searchbox", { name: "Search" }) as HTMLInputElement).value).toBe(
      "slab",
    );

    rerender(<AuditLogFilters filters={filters()} facets={facets} onChange={vi.fn()} />);

    expect((screen.getByRole("searchbox", { name: "Search" }) as HTMLInputElement).value).toBe("");
  });

  it("shows a message about the filters", () => {
    render(
      <AuditLogFilters
        filters={filters()}
        facets={facets}
        error="The start date must be on or before the end date."
        onChange={vi.fn()}
      />,
    );

    expect(screen.getByRole("alert").textContent).toBe(
      "The start date must be on or before the end date.",
    );
  });
});
