import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import type { AuditEntry } from "@/lib/audit-types";

import { AuditLogTable } from "./AuditLogTable";

function entry(change: Partial<AuditEntry> & { id: string }): AuditEntry {
  return {
    projectId: null,
    projectName: null,
    projectNumber: null,
    actorUserId: null,
    actorName: null,
    actorEmail: null,
    action: "TASK_CREATED",
    entityType: "TASK",
    entityId: null,
    oldValues: null,
    newValues: null,
    ipAddress: null,
    createdAt: "2026-03-10T23:30:00Z",
    ...change,
  };
}

const created = entry({
  id: "e1",
  projectId: "p1",
  projectName: "Riverside Lofts",
  projectNumber: "PRJ-0001",
  actorUserId: "u1",
  actorName: "Ann Builder",
  actorEmail: "ann@acme.test",
  entityId: "99999999-0000-0000-0000-000000000009",
  newValues: "TaskView[id=7, title=Pour slab, status=TODO]",
  ipAddress: "10.0.0.1",
});

const system = entry({
  id: "e2",
  action: "USER_REGISTERED",
  entityType: "AUTH",
  newValues: "RegisterView[email=zed@acme.test, passwordHash=[redacted]]",
  createdAt: "2026-03-12T08:00:00Z",
});

afterEach(() => {
  cleanup();
});

describe("AuditLogTable", () => {
  it("shows when, who, what and which project", () => {
    render(<AuditLogTable items={[created, system]} timeZone="Europe/Paris" />);

    const rows = screen.getAllByRole("row");

    // Header row, then one row per entry.
    expect(rows.length).toBe(3);

    const first = within(rows[1]);

    // 23:30 UTC on 10 March is already 11 March in Paris.
    expect(first.getByText("11 Mar 2026")).toBeTruthy();
    expect(first.getByText("00:30:00")).toBeTruthy();
    expect(first.getByText("Ann Builder")).toBeTruthy();
    expect(first.getByText("ann@acme.test")).toBeTruthy();
    expect(first.getByText("Task created")).toBeTruthy();
    expect(first.getByText("Task · Pour slab")).toBeTruthy();
    expect(first.getByText("Riverside Lofts")).toBeTruthy();
    expect(first.getByText("PRJ-0001")).toBeTruthy();
  });

  it("names actions with no signed-in user as the system", () => {
    render(<AuditLogTable items={[system]} timeZone="UTC" />);

    const row = within(screen.getAllByRole("row")[1]);

    expect(row.getByText("System")).toBeTruthy();
    expect(row.getByText("No signed-in user")).toBeTruthy();
    expect(row.getByText("Whole organization")).toBeTruthy();
    expect(row.getByText("User registered")).toBeTruthy();
  });

  it("opens the recorded values and closes them again", () => {
    render(<AuditLogTable items={[created]} timeZone="Europe/Paris" />);

    const toggle = screen.getByRole("button", {
      name: "Details of Task created on 11 Mar 2026, 00:30:00",
    });

    expect(toggle.getAttribute("aria-expanded")).toBe("false");
    expect(screen.queryByText("Pour slab")).toBeNull();

    fireEvent.click(toggle);

    expect(toggle.getAttribute("aria-expanded")).toBe("true");
    expect(screen.getByText("Title")).toBeTruthy();
    expect(screen.getByText("Status")).toBeTruthy();
    expect(screen.getByText("TODO")).toBeTruthy();
    expect(screen.getByText("10.0.0.1")).toBeTruthy();
    expect(screen.getByText("99999999-0000-0000-0000-000000000009")).toBeTruthy();
    expect(screen.getByText("11 Mar 2026, 00:30:00 (Europe/Paris)")).toBeTruthy();

    fireEvent.click(toggle);

    expect(toggle.getAttribute("aria-expanded")).toBe("false");
    expect(screen.queryByText("TODO")).toBeNull();
  });

  it("hides masked values behind a plain message", () => {
    render(<AuditLogTable items={[system]} timeZone="UTC" />);

    fireEvent.click(screen.getByRole("button", { name: /Details of User registered/ }));

    expect(screen.getByText("Hidden for security")).toBeTruthy();
    expect(screen.queryByText("[redacted]")).toBeNull();
    expect(screen.getByText("zed@acme.test")).toBeTruthy();
  });

  it("shows values it cannot read as they were recorded", () => {
    render(
      <AuditLogTable
        items={[entry({ id: "e3", newValues: "Something happened here" })]}
        timeZone="UTC"
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /Details of Task created/ }));

    expect(screen.getByText("Something happened here")).toBeTruthy();
  });

  it("says so when no values were recorded", () => {
    render(<AuditLogTable items={[entry({ id: "e4" })]} timeZone="UTC" />);

    fireEvent.click(screen.getByRole("button", { name: /Details of Task created/ }));

    expect(screen.getByText("No values were recorded for this action.")).toBeTruthy();
  });

  it("shows earlier values as Before and Recorded values as After", () => {
    render(
      <AuditLogTable
        items={[
          entry({
            id: "e5",
            action: "TASK_UPDATED",
            oldValues: "TaskView[status=TODO]",
            newValues: "TaskView[status=DONE]",
          }),
        ]}
        timeZone="UTC"
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /Details of Task updated/ }));

    expect(screen.getByText("Before")).toBeTruthy();
    expect(screen.getByText("After")).toBeTruthy();
    expect(screen.getByText("TODO")).toBeTruthy();
    expect(screen.getByText("DONE")).toBeTruthy();
  });
});
