import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OPEN_SEARCH_EVENT } from "@/lib/search";

import { AppSidebar } from "./AppSidebar";

const mocks = vi.hoisted(() => ({
  logout: vi.fn(async () => undefined),
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/app/projects/123",
}));

vi.mock("@/components/AuthProvider", () => ({
  useAuth: () => ({
    organizations: [
      { id: "org-1", name: "Acme Build" },
      { id: "org-2", name: "Beta Works" },
    ],
    logout: mocks.logout,
  }),
}));

function mobileNav() {
  return within(
    screen.getByRole("navigation", { name: "Mobile navigation" }),
  );
}

afterEach(() => {
  cleanup();
  window.localStorage.clear();
  mocks.logout.mockClear();
});

describe("AppSidebar mobile bar", () => {
  it("links every section and marks the current one", () => {
    render(<AppSidebar />);

    for (const label of [
      "Overview",
      "Projects",
      "Clients",
      "Suppliers",
      "Team",
      "Reports",
      "Notifications",
    ]) {
      expect(mobileNav().getByRole("link", { name: label })).toBeTruthy();
    }

    expect(
      mobileNav().getByRole("link", { name: "Projects" }).getAttribute("aria-current"),
    ).toBe("page");
    expect(
      mobileNav().getByRole("link", { name: "Clients" }).getAttribute("aria-current"),
    ).toBeNull();
  });

  it("opens the search palette", () => {
    const opened = vi.fn();

    window.addEventListener(OPEN_SEARCH_EVENT, opened);

    render(<AppSidebar />);

    fireEvent.click(mobileNav().getByRole("button", { name: "Search" }));

    window.removeEventListener(OPEN_SEARCH_EVENT, opened);

    expect(opened).toHaveBeenCalledTimes(1);
  });

  it("switches organization", () => {
    render(<AppSidebar />);

    fireEvent.change(
      mobileNav().getByRole("combobox", { name: "Organization" }),
      { target: { value: "org-2" } },
    );

    expect(window.localStorage.getItem("buildflow.organizationId")).toBe(
      "org-2",
    );
  });

  it("signs out", async () => {
    render(<AppSidebar />);

    fireEvent.click(mobileNav().getByRole("button", { name: "Sign out" }));

    await waitFor(() => {
      expect(mocks.logout).toHaveBeenCalledTimes(1);
    });
  });
});
