import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ProjectWorkspaceNav } from "./ProjectWorkspaceNav";

const mocks = vi.hoisted(() => ({
  pathname: "/app/projects/p1/site",
}));

vi.mock("next/navigation", () => ({
  usePathname: () => mocks.pathname,
}));

vi.mock("@/components/NotificationBell", () => ({
  NotificationBell: () => null,
}));

beforeEach(() => {
  // jsdom does not implement scrolling.
  window.HTMLElement.prototype.scrollIntoView = vi.fn();
});

afterEach(() => {
  cleanup();
  mocks.pathname = "/app/projects/p1/site";
});

describe("ProjectWorkspaceNav quick report link", () => {
  it("offers a quick report right after Site, on the Site page", () => {
    render(<ProjectWorkspaceNav projectId="p1" />);

    const labels = screen.getAllByRole("link").map((link) => link.textContent);

    expect(labels.slice(labels.indexOf("Site"), labels.indexOf("Site") + 3)).toEqual([
      "Site",
      "Quick report",
      "Documents",
    ]);
    expect(screen.getByRole("link", { name: "Quick report" }).getAttribute("href")).toBe(
      "/app/projects/p1/site/capture",
    );
  });

  it("keeps Site as the current section", () => {
    render(<ProjectWorkspaceNav projectId="p1" />);

    expect(screen.getByRole("link", { name: "Site" }).getAttribute("aria-current")).toBe("page");
    expect(screen.getByRole("link", { name: "Quick report" }).getAttribute("aria-current")).toBeNull();
  });

  it("also shows it when the address ends with a slash", () => {
    mocks.pathname = "/app/projects/p1/site/";

    render(<ProjectWorkspaceNav projectId="p1" />);

    expect(screen.getByRole("link", { name: "Quick report" })).toBeTruthy();
  });

  it.each(["/app/projects/p1", "/app/projects/p1/tasks", "/app/projects/p1/site/capture", "/somewhere/else"])(
    "does not show it on %s",
    (pathname) => {
      mocks.pathname = pathname;

      render(<ProjectWorkspaceNav projectId="p1" active="site" />);

      expect(screen.queryByRole("link", { name: "Quick report" })).toBeNull();
    },
  );
});
