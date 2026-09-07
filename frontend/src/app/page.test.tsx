import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import Home from "./page";

describe("BuildFlow home page", () => {

  it("renders the BuildFlow identity", () => {

    render(<Home />);

    expect(
      screen.getByText("BuildFlow")
    ).toBeInTheDocument();

    expect(
      screen.getByRole(
        "heading",
        {
          name: /Build better projects/i
        }
      )
    ).toBeInTheDocument();
  });
});
