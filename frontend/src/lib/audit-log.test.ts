import { describe, expect, it } from "vitest";

import {
  AUDIT_PAGE_SIZE,
  EMPTY_AUDIT_FILTERS,
  actionTone,
  activeFilterChips,
  actorDetail,
  actorLabel,
  actorOptionLabel,
  buildAuditQuery,
  canViewAuditLog,
  entrySubject,
  formatAuditTime,
  formatDay,
  hasActiveFilters,
  humanizeAction,
  humanizeEntityType,
  isIsoDay,
  parseAuditValues,
  projectOptionLabel,
  rangeLabel,
  validateAuditFilters,
} from "./audit-log";
import type { AuditFacets, AuditFilters } from "./audit-types";

const PROJECT = "22222222-0000-0000-0000-000000000001";
const PERSON = "11111111-0000-0000-0000-00000000000a";

function filters(change: Partial<AuditFilters> = {}): AuditFilters {
  return { ...EMPTY_AUDIT_FILTERS, ...change };
}

const facets: AuditFacets = {
  actions: ["TASK_CREATED"],
  entityTypes: ["TASK"],
  actors: [{ userId: PERSON, name: "Ann Builder", email: "ann@acme.test" }],
  projects: [{ projectId: PROJECT, name: "Riverside Lofts", projectNumber: "PRJ-0001" }],
};

describe("canViewAuditLog", () => {
  it("allows owners and administrators only", () => {
    expect(canViewAuditLog("OWNER")).toBe(true);
    expect(canViewAuditLog("ADMIN")).toBe(true);
    expect(canViewAuditLog("PROJECT_MANAGER")).toBe(false);
    expect(canViewAuditLog("ACCOUNTANT")).toBe(false);
    expect(canViewAuditLog("CLIENT")).toBe(false);
    expect(canViewAuditLog("owner")).toBe(false);
    expect(canViewAuditLog(undefined)).toBe(false);
    expect(canViewAuditLog(null)).toBe(false);
  });
});

describe("isIsoDay", () => {
  it("accepts real days", () => {
    expect(isIsoDay("2026-03-31")).toBe(true);
    expect(isIsoDay("2028-02-29")).toBe(true);
  });

  it("rejects everything else", () => {
    for (const value of ["", "abc", "2026-3-1", "2026-02-30", "2026-13-01", "2027-02-29", "31/03/2026", "0050-01-01"]) {
      expect(isIsoDay(value)).toBe(false);
    }
  });
});

describe("hasActiveFilters", () => {
  it("is false when nothing is chosen", () => {
    expect(hasActiveFilters(EMPTY_AUDIT_FILTERS)).toBe(false);
    expect(hasActiveFilters(filters({ q: "   " }))).toBe(false);
  });

  it("is true when anything is chosen", () => {
    expect(hasActiveFilters(filters({ q: "slab" }))).toBe(true);
    expect(hasActiveFilters(filters({ from: "2026-03-01" }))).toBe(true);
  });
});

describe("validateAuditFilters", () => {
  it("accepts empty filters and ordered dates", () => {
    expect(validateAuditFilters(EMPTY_AUDIT_FILTERS)).toBeNull();
    expect(validateAuditFilters(filters({ from: "2026-03-01", to: "2026-03-01" }))).toBeNull();
    expect(validateAuditFilters(filters({ from: "2026-03-01" }))).toBeNull();
    expect(validateAuditFilters(filters({ to: "2026-03-01" }))).toBeNull();
  });

  it("explains what is wrong", () => {
    expect(validateAuditFilters(filters({ from: "2026-03-12", to: "2026-03-11" }))).toBe(
      "The start date must be on or before the end date.",
    );
    expect(validateAuditFilters(filters({ from: "2026-02-30" }))).toBe(
      "The start date is not a valid date.",
    );
    expect(validateAuditFilters(filters({ to: "nope" }))).toBe("The end date is not a valid date.");
  });
});

describe("buildAuditQuery", () => {
  it("asks for the first page when nothing is chosen", () => {
    expect(buildAuditQuery(EMPTY_AUDIT_FILTERS, 0)).toBe(`?page=0&size=${AUDIT_PAGE_SIZE}`);
  });

  it("sends every chosen filter", () => {
    expect(
      buildAuditQuery(
        filters({
          q: "pour slab",
          projectId: PROJECT,
          actorUserId: PERSON,
          entityType: "TASK",
          action: "TASK_UPDATED",
          from: "2026-03-01",
          to: "2026-03-31",
        }),
        2,
        10,
      ),
    ).toBe(
      `?page=2&size=10&q=pour+slab&projectId=${PROJECT}&actorUserId=${PERSON}` +
        "&entityType=TASK&action=TASK_UPDATED&from=2026-03-01&to=2026-03-31",
    );
  });

  it("trims and encodes the text", () => {
    expect(buildAuditQuery(filters({ q: "  50% & more  " }), 0)).toBe(
      `?page=0&size=${AUDIT_PAGE_SIZE}&q=50%25+%26+more`,
    );
  });

  it("cuts very long text", () => {
    const query = buildAuditQuery(filters({ q: "x".repeat(500) }), 0);

    expect(query).toContain(`q=${"x".repeat(100)}`);
    expect(query).not.toContain("x".repeat(101));
  });

  it("leaves out values that are not ids or days", () => {
    expect(
      buildAuditQuery(
        filters({ projectId: "not-a-uuid", actorUserId: "123", from: "yesterday", to: "2026-02-30" }),
        0,
      ),
    ).toBe(`?page=0&size=${AUDIT_PAGE_SIZE}`);
  });

  it("keeps the page and size in range", () => {
    expect(buildAuditQuery(EMPTY_AUDIT_FILTERS, -4, 0)).toBe("?page=0&size=1");
    expect(buildAuditQuery(EMPTY_AUDIT_FILTERS, 2.9, 5000)).toBe("?page=2&size=100");
    expect(buildAuditQuery(EMPTY_AUDIT_FILTERS, Number.NaN, Number.NaN)).toBe(
      `?page=0&size=${AUDIT_PAGE_SIZE}`,
    );
  });
});

describe("words", () => {
  it("turns codes into sentences", () => {
    expect(humanizeAction("CHANGE_ORDER_CREATED")).toBe("Change order created");
    expect(humanizeAction("task-updated")).toBe("Task updated");
    expect(humanizeAction("")).toBe("Unknown action");
    expect(humanizeEntityType("SITE_REPORT")).toBe("Site report");
    expect(humanizeEntityType("  ")).toBe("Record");
  });

  it("colours actions by their last word", () => {
    expect(actionTone("CHANGE_ORDER_APPROVED")).toBe("success");
    expect(actionTone("INCIDENT_RESOLVED")).toBe("success");
    expect(actionTone("TASK_DELETED")).toBe("danger");
    expect(actionTone("INVITATION_REVOKED")).toBe("danger");
    expect(actionTone("CHANGE_ORDER_SUBMITTED")).toBe("warning");
    expect(actionTone("TASK_CREATED")).toBe("info");
    expect(actionTone("USER_REGISTERED")).toBe("info");
    expect(actionTone("TASK_UPDATED")).toBe("neutral");
    expect(actionTone("")).toBe("neutral");
  });
});

describe("people and projects", () => {
  const named = { actorUserId: PERSON, actorName: "Ann Builder", actorEmail: "ann@acme.test" };

  it("names the person who acted", () => {
    expect(actorLabel(named)).toBe("Ann Builder");
    expect(actorLabel({ ...named, actorName: null })).toBe("ann@acme.test");
    expect(actorLabel({ ...named, actorName: null, actorEmail: null })).toBe("Unknown person");
    expect(actorLabel({ actorUserId: null, actorName: null, actorEmail: null })).toBe("System");
  });

  it("shows the email under a name", () => {
    expect(actorDetail(named)).toBe("ann@acme.test");
    expect(actorDetail({ ...named, actorName: null })).toBeNull();
    expect(actorDetail({ actorUserId: null, actorName: null, actorEmail: null })).toBe(
      "No signed-in user",
    );
  });

  it("labels the filter choices", () => {
    expect(actorOptionLabel({ userId: PERSON, name: "Ann Builder", email: "ann@acme.test" })).toBe(
      "Ann Builder (ann@acme.test)",
    );
    expect(actorOptionLabel({ userId: PERSON, name: null, email: "ann@acme.test" })).toBe(
      "ann@acme.test",
    );
    expect(projectOptionLabel(facets.projects[0])).toBe("PRJ-0001 · Riverside Lofts");
  });
});

describe("formatAuditTime", () => {
  it("reads the instant in the given time zone", () => {
    expect(formatAuditTime("2026-03-10T23:30:00Z", "Europe/Paris")).toEqual({
      date: "11 Mar 2026",
      time: "00:30:00",
      label: "11 Mar 2026, 00:30:00",
    });
    expect(formatAuditTime("2026-03-10T23:30:00Z", "UTC").date).toBe("10 Mar 2026");
  });

  it("writes midnight as 00, not 24", () => {
    expect(formatAuditTime("2026-03-10T23:00:00Z", "Europe/Paris").time).toBe("00:00:00");
  });

  it("follows the clock change", () => {
    expect(formatAuditTime("2026-03-29T00:30:00Z", "Europe/Paris").time).toBe("01:30:00");
    expect(formatAuditTime("2026-03-29T01:30:00Z", "Europe/Paris").time).toBe("03:30:00");
  });

  it("falls back to UTC for an unknown time zone", () => {
    expect(formatAuditTime("2026-03-10T23:30:00Z", "Not/AZone").label).toBe("10 Mar 2026, 23:30:00");
    expect(formatAuditTime("2026-03-10T23:30:00Z", "").label).toBe("10 Mar 2026, 23:30:00");
  });

  it("shows a dash when there is no valid time", () => {
    expect(formatAuditTime(null, "UTC").label).toBe("—");
    expect(formatAuditTime("garbage", "UTC").label).toBe("—");
  });
});

describe("formatDay", () => {
  it("writes a day with its month name", () => {
    expect(formatDay("2026-03-11")).toBe("11 Mar 2026");
    expect(formatDay("2026-09-05")).toBe("05 Sep 2026");
    expect(formatDay("nope")).toBe("nope");
  });
});

describe("parseAuditValues", () => {
  it("has nothing to show for empty values", () => {
    expect(parseAuditValues(null)).toEqual({ kind: "none" });
    expect(parseAuditValues("   ")).toEqual({ kind: "none" });
  });

  it("reads a record", () => {
    expect(parseAuditValues("TaskView[id=7, title=Pour slab, status=TODO]")).toEqual({
      kind: "fields",
      typeName: "TaskView",
      fields: [
        { key: "id", label: "ID", value: "7" },
        { key: "title", label: "Title", value: "Pour slab" },
        { key: "status", label: "Status", value: "TODO" },
      ],
    });
  });

  it("labels camel-case names and keeps commas inside a value", () => {
    expect(
      parseAuditValues("ChangeOrderView[title=Pour slab, level 2, costImpact=1200, entityId=null]"),
    ).toEqual({
      kind: "fields",
      typeName: "ChangeOrderView",
      fields: [
        { key: "title", label: "Title", value: "Pour slab, level 2" },
        { key: "costImpact", label: "Cost impact", value: "1200" },
        { key: "entityId", label: "Entity ID", value: "—" },
      ],
    });
  });

  it("keeps nested records whole", () => {
    expect(parseAuditValues("Result[user=User[id=1, email=a@b.c], count=2]")).toEqual({
      kind: "fields",
      typeName: "Result",
      fields: [
        { key: "user", label: "User", value: "User[id=1, email=a@b.c]" },
        { key: "count", label: "Count", value: "2" },
      ],
    });
  });

  it("reads masked values", () => {
    expect(parseAuditValues("Login[email=a@b.c, token=[redacted]]")).toEqual({
      kind: "fields",
      typeName: "Login",
      fields: [
        { key: "email", label: "Email", value: "a@b.c" },
        { key: "token", label: "Token", value: "[redacted]" },
      ],
    });
  });

  it("reads an empty record", () => {
    expect(parseAuditValues("Empty[]")).toEqual({ kind: "fields", typeName: "Empty", fields: [] });
  });

  it("reads a JSON object", () => {
    expect(parseAuditValues('{"name":"Ann","count":3,"note":null,"tags":["a","b"]}')).toEqual({
      kind: "fields",
      typeName: null,
      fields: [
        { key: "name", label: "Name", value: "Ann" },
        { key: "count", label: "Count", value: "3" },
        { key: "note", label: "Note", value: "—" },
        { key: "tags", label: "Tags", value: '["a","b"]' },
      ],
    });
  });

  it("leaves anything else as plain text", () => {
    expect(parseAuditValues("Something happened here")).toEqual({
      kind: "text",
      text: "Something happened here",
    });
    expect(parseAuditValues("[1, 2, 3]")).toEqual({ kind: "text", text: "[1, 2, 3]" });
    expect(parseAuditValues('{"broken": ')).toEqual({ kind: "text", text: '{"broken":' });
    // A value cut off by the server's length limit has no closing bracket.
    expect(parseAuditValues("TaskView[id=7, title=Pour sl")).toEqual({
      kind: "text",
      text: "TaskView[id=7, title=Pour sl",
    });
    expect(parseAuditValues("Odd[no equals here]")).toEqual({
      kind: "text",
      text: "Odd[no equals here]",
    });
  });
});

describe("entrySubject", () => {
  it("picks a title or name from the values", () => {
    expect(entrySubject({ newValues: "TaskView[id=7, title=Pour slab, status=TODO]" })).toBe("Pour slab");
    expect(entrySubject({ newValues: "ClientView[id=1, name=Acme Ltd]" })).toBe("Acme Ltd");
    expect(entrySubject({ newValues: '{"subject":"Roof leak"}' })).toBe("Roof leak");
  });

  it("falls back to a number", () => {
    expect(entrySubject({ newValues: "InvoiceView[id=1, invoiceNumber=INV-0007, total=10]" })).toBe("INV-0007");
  });

  it("returns null when nothing fits", () => {
    expect(entrySubject({ newValues: null })).toBeNull();
    expect(entrySubject({ newValues: "plain text" })).toBeNull();
    expect(entrySubject({ newValues: "TaskView[id=7, status=TODO]" })).toBeNull();
    expect(entrySubject({ newValues: "TaskView[id=7, title=null]" })).toBeNull();
  });

  it("shortens long subjects", () => {
    const subject = entrySubject({ newValues: `NoteView[title=${"a".repeat(200)}]` });

    expect(subject?.length).toBe(80);
    expect(subject?.endsWith("…")).toBe(true);
  });
});

describe("activeFilterChips", () => {
  it("has no chips without filters", () => {
    expect(activeFilterChips(EMPTY_AUDIT_FILTERS, facets)).toEqual([]);
  });

  it("describes each filter in plain words", () => {
    expect(
      activeFilterChips(
        filters({
          q: " slab ",
          projectId: PROJECT,
          actorUserId: PERSON,
          entityType: "SITE_REPORT",
          action: "TASK_UPDATED",
          from: "2026-03-01",
          to: "2026-03-31",
        }),
        facets,
      ),
    ).toEqual([
      { key: "q", label: "Search: “slab”" },
      { key: "projectId", label: "Project: PRJ-0001 · Riverside Lofts" },
      { key: "actorUserId", label: "Person: Ann Builder" },
      { key: "entityType", label: "Kind: Site report" },
      { key: "action", label: "Action: Task updated" },
      { key: "from", label: "From: 01 Mar 2026" },
      { key: "to", label: "Until: 31 Mar 2026" },
    ]);
  });

  it("still labels choices before the lists have loaded", () => {
    expect(activeFilterChips(filters({ projectId: PROJECT, actorUserId: PERSON }), null)).toEqual([
      { key: "projectId", label: "Project: selected project" },
      { key: "actorUserId", label: "Person: selected person" },
    ]);
  });
});

describe("rangeLabel", () => {
  const entry = {} as never;

  it("says which entries are shown", () => {
    expect(rangeLabel({ page: 0, size: 25, totalElements: 60, items: Array(25).fill(entry) })).toBe(
      "Showing 1–25 of 60",
    );
    expect(rangeLabel({ page: 2, size: 25, totalElements: 60, items: Array(10).fill(entry) })).toBe(
      "Showing 51–60 of 60",
    );
    expect(rangeLabel({ page: 0, size: 25, totalElements: 1234, items: Array(25).fill(entry) })).toBe(
      "Showing 1–25 of 1,234",
    );
  });

  it("handles no entries and a page past the end", () => {
    expect(rangeLabel({ page: 0, size: 25, totalElements: 0, items: [] })).toBe("No entries");
    expect(rangeLabel({ page: 9, size: 25, totalElements: 60, items: [] })).toBe(
      "No entries on this page (60 in total)",
    );
  });
});
