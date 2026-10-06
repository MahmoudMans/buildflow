import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { AuditEntry, AuditFacets, AuditPage } from "@/lib/audit-types";

import AuditPageRoute from "./page";

const PROJECT = "22222222-0000-0000-0000-000000000001";

const mocks = vi.hoisted(() => ({
  replace: vi.fn(),
  apiGet: vi.fn(),
  auth: {
    user: { id: "u1" } as { id: string } | null,
    loading: false,
    organizations: [
      {
        id: "org-1",
        name: "Acme Build",
        currency: "EUR",
        timezone: "Europe/Paris",
        role: "OWNER",
      },
    ],
  },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mocks.replace }),
  usePathname: () => "/app/audit",
}));

vi.mock("@/components/AuthProvider", () => ({
  useAuth: () => mocks.auth,
}));

vi.mock("@/components/AppSidebar", () => ({
  AppSidebar: () => null,
}));

vi.mock("@/components/OrganizationSelect", () => ({
  OrganizationSelect: () => null,
}));

vi.mock("@/lib/api", () => ({
  ApiClientError: class ApiClientError extends Error {},
  apiGet: mocks.apiGet,
}));

const facets: AuditFacets = {
  actions: ["TASK_CREATED"],
  entityTypes: ["TASK"],
  actors: [{ userId: "u1", name: "Ann Builder", email: "ann@acme.test" }],
  projects: [{ projectId: PROJECT, name: "Riverside Lofts", projectNumber: "PRJ-0001" }],
};

function entry(id: string, action = "TASK_CREATED"): AuditEntry {
  return {
    id,
    projectId: PROJECT,
    projectName: "Riverside Lofts",
    projectNumber: "PRJ-0001",
    actorUserId: "u1",
    actorName: "Ann Builder",
    actorEmail: "ann@acme.test",
    action,
    entityType: "TASK",
    entityId: null,
    oldValues: null,
    newValues: "TaskView[id=7, title=Pour slab, status=TODO]",
    ipAddress: "10.0.0.1",
    createdAt: "2026-03-10T23:30:00Z",
  };
}

function results(change: Partial<AuditPage> = {}): AuditPage {
  return {
    items: [entry("e1")],
    page: 0,
    size: 25,
    totalElements: 1,
    totalPages: 1,
    ...change,
  };
}

function answer(search: (path: string) => AuditPage | Promise<AuditPage>) {
  mocks.apiGet.mockImplementation(async (path: string) =>
    path.startsWith("/audit/facets") ? facets : search(path),
  );
}

function searchCalls(): string[] {
  return mocks.apiGet.mock.calls
    .map((call: unknown[]) => String(call[0]))
    .filter((path: string) => path.startsWith("/audit/search"));
}

beforeEach(() => {
  window.localStorage.clear();
  mocks.auth.user = { id: "u1" };
  mocks.auth.loading = false;
  mocks.auth.organizations[0].role = "OWNER";
  answer(() => results());
});

afterEach(() => {
  cleanup();
  mocks.apiGet.mockReset();
  mocks.replace.mockReset();
  window.localStorage.clear();
});

describe("Audit log page", () => {
  it("sends people who are not signed in to the login page", () => {
    mocks.auth.user = null;

    render(<AuditPageRoute />);

    expect(mocks.replace).toHaveBeenCalledWith("/login");
    expect(mocks.apiGet).not.toHaveBeenCalled();
  });

  it("explains that it is for owners and administrators, and asks for nothing", async () => {
    mocks.auth.organizations[0].role = "ACCOUNTANT";

    render(<AuditPageRoute />);

    expect(screen.getByText("The audit log is for owners and administrators")).toBeTruthy();

    await new Promise((resolve) => setTimeout(resolve, 30));

    expect(mocks.apiGet).not.toHaveBeenCalled();
  });

  it("loads the first page and the filter lists for an owner", async () => {
    render(<AuditPageRoute />);

    expect(await screen.findByText("Pour slab", { exact: false })).toBeTruthy();
    expect(screen.getByText("Ann Builder")).toBeTruthy();

    expect(mocks.apiGet).toHaveBeenCalledWith("/audit/search?page=0&size=25", "org-1");
    expect(mocks.apiGet).toHaveBeenCalledWith("/audit/facets", "org-1");

    expect(screen.getByText("Times are shown in the organization's time zone (Europe/Paris).")).toBeTruthy();
  });

  it("searches again, from the first page, when a filter changes", async () => {
    render(<AuditPageRoute />);

    await screen.findByRole("option", { name: "PRJ-0001 · Riverside Lofts" });

    fireEvent.change(screen.getByRole("combobox", { name: "Project" }), {
      target: { value: PROJECT },
    });

    await waitFor(() => {
      expect(searchCalls()).toContain(`/audit/search?page=0&size=25&projectId=${PROJECT}`);
    });

    expect(
      screen.getByRole("button", { name: "Remove filter Project: PRJ-0001 · Riverside Lofts" }),
    ).toBeTruthy();
  });

  it("moves to the next page", async () => {
    answer((path) =>
      path.includes("page=1")
        ? results({ page: 1, items: [entry("e2", "TASK_UPDATED")], totalElements: 30 , totalPages: 2 })
        : results({ totalElements: 30, totalPages: 2 }),
    );

    render(<AuditPageRoute />);

    fireEvent.click(await screen.findByRole("button", { name: "Next" }));

    await waitFor(() => {
      expect(searchCalls()).toContain("/audit/search?page=1&size=25");
    });

    expect(await screen.findByText("Task updated")).toBeTruthy();
  });

  it("says when nothing matches, and clears the filters", async () => {
    answer((path) =>
      path.includes("q=nothing")
        ? results({ items: [], totalElements: 0, totalPages: 0 })
        : results(),
    );

    render(<AuditPageRoute />);

    await screen.findByText("Ann Builder");

    fireEvent.change(screen.getByRole("searchbox", { name: "Search" }), {
      target: { value: "nothing" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search" }));

    expect(await screen.findByText("No entries match these filters")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Clear filters" }));

    expect(await screen.findByText("Ann Builder")).toBeTruthy();
    expect((screen.getByRole("searchbox", { name: "Search" }) as HTMLInputElement).value).toBe("");
  });

  it("says when nothing has been recorded yet", async () => {
    answer(() => results({ items: [], totalElements: 0, totalPages: 0 }));

    render(<AuditPageRoute />);

    expect(await screen.findByText("Nothing has been recorded yet")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Clear filters" })).toBeNull();
  });

  it("does not search while the dates are the wrong way round", async () => {
    render(<AuditPageRoute />);

    await screen.findByText("Ann Builder");

    fireEvent.change(screen.getByLabelText("From"), { target: { value: "2026-03-12" } });

    await waitFor(() => {
      expect(searchCalls()).toContain("/audit/search?page=0&size=25&from=2026-03-12");
    });

    const before = searchCalls().length;

    fireEvent.change(screen.getByLabelText("To"), { target: { value: "2026-03-11" } });

    expect(await screen.findByText("The start date must be on or before the end date.")).toBeTruthy();

    await new Promise((resolve) => setTimeout(resolve, 30));

    expect(searchCalls().length).toBe(before);
  });

  it("shows an error with a way to try again", async () => {
    let attempts = 0;

    mocks.apiGet.mockImplementation(async (path: string) => {
      if (path.startsWith("/audit/facets")) {
        return facets;
      }

      attempts += 1;

      if (attempts === 1) {
        throw new Error("boom");
      }

      return results();
    });

    render(<AuditPageRoute />);

    expect(await screen.findByText("Could not load the audit log")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByText("Ann Builder")).toBeTruthy();
    expect(screen.queryByText("Could not load the audit log")).toBeNull();
  });

  it("still works when the filter lists cannot be loaded", async () => {
    mocks.apiGet.mockImplementation(async (path: string) => {
      if (path.startsWith("/audit/facets")) {
        throw new Error("no lists");
      }

      return results();
    });

    render(<AuditPageRoute />);

    expect(await screen.findByText("Ann Builder")).toBeTruthy();
    expect(
      await screen.findByText(
        "The lists of projects and people could not be loaded. You can still search by text and date.",
      ),
    ).toBeTruthy();
  });
});
