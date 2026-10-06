import { describe, expect, it } from "vitest";

import {
  buildSections,
  filterQuickLinks,
  hitHref,
  statusLabel,
  type SearchHit,
} from "./search";

function hit(overrides: Partial<SearchHit>): SearchHit {
  return {
    kind: "PROJECT",
    id: "11111111-1111-1111-1111-111111111111",
    projectId: null,
    title: "Title",
    subtitle: "Subtitle",
    status: null,
    ...overrides,
  };
}

describe("global search helpers", () => {
  it("links every kind of result to the page that shows it", () => {
    const project = "22222222-2222-2222-2222-222222222222";

    expect(hitHref(hit({ kind: "PROJECT", id: project, projectId: project }))).toBe(
      `/app/projects/${project}`,
    );
    expect(hitHref(hit({ kind: "TASK", id: "t1", projectId: project }))).toBe(
      `/app/projects/${project}/tasks/t1`,
    );
    expect(hitHref(hit({ kind: "CLIENT", id: "c1" }))).toBe("/app/clients/c1");
    expect(hitHref(hit({ kind: "SUPPLIER", id: "s1" }))).toBe("/app/suppliers");
    expect(hitHref(hit({ kind: "INVOICE", id: "i1", projectId: project }))).toBe(
      `/app/projects/${project}/commercial`,
    );
    expect(
      hitHref(hit({ kind: "PURCHASE_ORDER", id: "p1", projectId: project })),
    ).toBe(`/app/projects/${project}/procurement`);
  });

  it("falls back to the projects list when a project id is missing", () => {
    expect(hitHref(hit({ kind: "TASK", projectId: null }))).toBe("/app/projects");
    expect(hitHref(hit({ kind: "INVOICE", projectId: null }))).toBe(
      "/app/projects",
    );
  });

  it("filters pages by name and shows all of them for an empty query", () => {
    expect(filterQuickLinks("")).toHaveLength(7);
    expect(filterQuickLinks("  ")).toHaveLength(7);
    expect(filterQuickLinks("TEAM").map((link) => link.id)).toEqual(["team"]);
    expect(filterQuickLinks("zzz")).toEqual([]);
  });

  it("groups results by kind in a fixed order and skips empty groups", () => {
    const sections = buildSections("harbour", [
      hit({ kind: "INVOICE", id: "i1", title: "INV-1", projectId: "p" }),
      hit({ kind: "TASK", id: "t1", title: "Task", projectId: "p" }),
      hit({ kind: "PROJECT", id: "p", title: "Project", status: "ACTIVE" }),
      hit({ kind: "TASK", id: "t2", title: "Task 2", projectId: "p" }),
    ]);

    expect(sections.map((section) => section.id)).toEqual([
      "PROJECT",
      "TASK",
      "INVOICE",
    ]);

    expect(sections[1].items.map((item) => item.title)).toEqual([
      "Task",
      "Task 2",
    ]);

    expect(sections[0].items[0].status).toBe("ACTIVE");
    expect(sections[1].items[0].status).toBeNull();
  });

  it("puts matching pages before the results", () => {
    const sections = buildSections("pro", [
      hit({ kind: "PROJECT", id: "p", title: "Pro Tower", projectId: "p" }),
    ]);

    expect(sections.map((section) => section.id)).toEqual(["pages", "PROJECT"]);
    expect(sections[0].items.map((item) => item.title)).toEqual(["Projects"]);
  });

  it("makes item keys unique across kinds", () => {
    const sections = buildSections("x1", [
      hit({ kind: "TASK", id: "same", projectId: "p" }),
      hit({ kind: "CLIENT", id: "same" }),
    ]);

    const keys = sections.flatMap((section) =>
      section.items.map((item) => item.key),
    );

    expect(new Set(keys).size).toBe(keys.length);
  });

  it("turns statuses into readable labels", () => {
    expect(statusLabel("PARTIALLY_PAID")).toBe("Partially Paid");
    expect(statusLabel(null)).toBe("");
  });
});
