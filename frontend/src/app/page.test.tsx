import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import HomePage from "./page";

const CAPABILITIES = [
  "Project management",
  "Cost control",
  "Site operations",
  "Commercial management",
  "Project insights",
  "Client portal",
];

const ROLES = [
  "Owner",
  "Project Manager",
  "Site Manager",
  "Accountant",
  "Client",
];

afterEach(() => {
  cleanup();
});

describe("BuildFlow portfolio homepage", () => {
  it("presents the headline and the demo entry point", () => {
    render(<HomePage />);

    const heading = screen.getByRole("heading", { level: 1 });

    expect(heading.textContent).toContain("Projects in sync.");
    expect(heading.textContent).toContain("Costs in view.");

    const demoLinks = screen.getAllByRole("link", { name: /explore demo/i });

    expect(demoLinks.length).toBeGreaterThan(0);

    for (const link of demoLinks) {
      expect(link.getAttribute("href")).toBe("/demo");
    }
  });

  it("lists the product capabilities", () => {
    render(<HomePage />);

    for (const capability of CAPABILITIES) {
      expect(
        screen.getByRole("heading", { level: 3, name: capability }),
      ).toBeTruthy();
    }
  });

  it("lists the five demo roles", () => {
    render(<HomePage />);

    for (const role of ROLES) {
      expect(
        screen.getByRole("heading", { level: 3, name: role }),
      ).toBeTruthy();
    }
  });

  it("links to the source code and the architecture case study", () => {
    render(<HomePage />);

    const githubLinks = screen.getAllByRole("link", { name: /github/i });

    expect(
      githubLinks.some(
        (link) =>
          link.getAttribute("href") ===
          "https://github.com/MahmoudMans/buildflow",
      ),
    ).toBe(true);

    const architectureLinks = screen.getAllByRole("link", {
      name: /architecture/i,
    });

    expect(
      architectureLinks.some(
        (link) => link.getAttribute("href") === "/portfolio",
      ),
    ).toBe(true);
  });
});
