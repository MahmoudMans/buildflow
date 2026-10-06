import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { AppSidebar } from "./AppSidebar";

const mocks = vi.hoisted(() => ({
  roles: ["OWNER", "CLIENT"] as string[],
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/app/projects/123",
}));

vi.mock("@/components/AuthProvider", () => ({
  useAuth: () => ({
    organizations: [
      { id: "org-1", name: "Acme Build", role: mocks.roles[0] },
      { id: "org-2", name: "Beta Works", role: mocks.roles[1] },
    ],
    logout: vi.fn(async () => undefined),
  }),
}));

function desktopNav() {
  return within(screen.getByRole("navigation", { name: "Primary" }));
}

function mobileNav() {
  return within(screen.getByRole("navigation", { name: "Mobile navigation" }));
}

beforeEach(() => {
  window.localStorage.clear();
});

afterEach(() => {
  cleanup();
  window.localStorage.clear();
  mocks.roles = ["OWNER", "CLIENT"];
});

describe("AppSidebar audit log entry", () => {
  it("shows the audit log to an owner, in both menus", () => {
    render(<AppSidebar />);

    const desktop = desktopNav().getByRole("link", { name: "Audit log" });
    const mobile = mobileNav().getByRole("link", { name: "Audit log" });

    expect(desktop.getAttribute("href")).toBe("/app/audit");
    expect(mobile.getAttribute("href")).toBe("/app/audit");
  });

  it("shows it to an administrator", () => {
    mocks.roles = ["ADMIN", "CLIENT"];

    render(<AppSidebar />);

    expect(desktopNav().getByRole("link", { name: "Audit log" })).toBeTruthy();
  });

  it("hides it from everyone else", () => {
    for (const role of ["PROJECT_MANAGER", "SITE_MANAGER", "WORKER", "ACCOUNTANT", "CLIENT"]) {
      mocks.roles = [role, role];

      render(<AppSidebar />);

      expect(desktopNav().queryByRole("link", { name: "Audit log" })).toBeNull();
      expect(mobileNav().queryByRole("link", { name: "Audit log" })).toBeNull();

      cleanup();
    }
  });

  it("follows the selected organization", () => {
    render(<AppSidebar />);

    expect(mobileNav().queryByRole("link", { name: "Audit log" })).toBeTruthy();

    // Beta Works is a client role: no audit log there.
    fireEvent.change(mobileNav().getByRole("combobox", { name: "Organization" }), {
      target: { value: "org-2" },
    });

    expect(mobileNav().queryByRole("link", { name: "Audit log" })).toBeNull();
    expect(desktopNav().queryByRole("link", { name: "Audit log" })).toBeNull();

    fireEvent.change(mobileNav().getByRole("combobox", { name: "Organization" }), {
      target: { value: "org-1" },
    });

    expect(mobileNav().queryByRole("link", { name: "Audit log" })).toBeTruthy();
  });
});
