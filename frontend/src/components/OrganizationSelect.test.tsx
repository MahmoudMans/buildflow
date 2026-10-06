import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OrganizationSelect } from "./OrganizationSelect";

const mocks = vi.hoisted(() => ({
  organizations: [
    { id: "org-1", name: "Acme Build" },
    { id: "org-2", name: "Beta Works" },
  ],
}));

vi.mock("@/components/AuthProvider", () => ({
  useAuth: () => ({ organizations: mocks.organizations }),
}));

afterEach(() => {
  cleanup();
  window.localStorage.clear();
});

describe("OrganizationSelect", () => {
  it("lists the organizations and starts on the first one", () => {
    render(<OrganizationSelect />);

    const select = screen.getByRole("combobox") as HTMLSelectElement;

    expect(Array.from(select.options).map((option) => option.textContent)).toEqual(
      ["Acme Build", "Beta Works"],
    );
    expect(select.value).toBe("org-1");
  });

  it("stores the choice so the rest of the app follows it", async () => {
    render(<OrganizationSelect />);

    const select = screen.getByRole("combobox") as HTMLSelectElement;

    fireEvent.change(select, { target: { value: "org-2" } });

    expect(window.localStorage.getItem("buildflow.organizationId")).toBe(
      "org-2",
    );

    await waitFor(() => {
      expect(select.value).toBe("org-2");
    });
  });

  it("can be disabled while a form is saving", () => {
    render(<OrganizationSelect disabled />);

    expect((screen.getByRole("combobox") as HTMLSelectElement).disabled).toBe(
      true,
    );
  });
});
