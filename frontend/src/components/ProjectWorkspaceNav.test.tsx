import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ProjectWorkspaceNav } from "./ProjectWorkspaceNav";

const mocks = vi.hoisted(() => ({
  pathname: "/app/projects/p1",
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
  mocks.pathname = "/app/projects/p1";
});

describe("ProjectWorkspaceNav", () => {
  it("links the timeline between milestones and the money pages", () => {
    render(<ProjectWorkspaceNav projectId="p1" />);

    const labels = screen
      .getAllByRole("link")
      .map((link) => link.textContent);

    expect(labels.slice(1, 5)).toEqual([
      "Tasks",
      "Milestones",
      "Timeline",
      "Budget",
    ]);
    expect(
      screen.getByRole("link", { name: "Timeline" }).getAttribute("href"),
    ).toBe("/app/projects/p1/timeline");
  });

  it("marks the timeline as current on its own page", () => {
    mocks.pathname = "/app/projects/p1/timeline";

    render(<ProjectWorkspaceNav projectId="p1" />);

    expect(
      screen.getByRole("link", { name: "Timeline" }).getAttribute("aria-current"),
    ).toBe("page");
    expect(
      screen.getByRole("link", { name: "Milestones" }).getAttribute("aria-current"),
    ).toBeNull();
  });

  it("falls back to the section a page passes in", () => {
    mocks.pathname = "/somewhere/else";

    render(<ProjectWorkspaceNav projectId="p1" active="timeline" />);

    expect(
      screen.getByRole("link", { name: "Timeline" }).getAttribute("aria-current"),
    ).toBe("page");
  });
});
