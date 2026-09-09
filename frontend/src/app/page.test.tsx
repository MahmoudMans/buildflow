import {
  render,
  screen,
} from "@testing-library/react";

import {
  describe,
  expect,
  it,
} from "vitest";

import HomePage from "./page";

describe(
  "BuildFlow portfolio homepage",
  () => {
    it(
      "presents the product, portfolio capabilities and demo entry point",
      () => {
        render(
          <HomePage />
        );

        const heading =
          screen.getByRole(
            "heading",
            {
              level: 1,
            }
          );

        expect(
          heading.textContent
        ).toContain(
          "Build better projects."
        );

        expect(
          heading.textContent
        ).toContain(
          "Control every cost."
        );

        const demoLinks =
          screen.getAllByRole(
            "link",
            {
              name:
                /explore demo/i,
            }
          );

        expect(
          demoLinks.length
        ).toBeGreaterThan(
          0
        );

        expect(
          demoLinks[0]
            .getAttribute(
              "href"
            )
        ).toBe(
          "/demo"
        );

        expect(
          screen.getByText(
            "Project management"
          )
        ).toBeTruthy();

        expect(
          screen.getByText(
            "Client portal"
          )
        ).toBeTruthy();

        expect(
          screen.getByText(
            "Analytics"
          )
        ).toBeTruthy();

        expect(
          screen.getByText(
            "0"
          )
        ).toBeTruthy();

        expect(
          screen.getByText(
            "paid external APIs"
          )
        ).toBeTruthy();
      }
    );
  }
);
