import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { AuditEntry, AuditPage } from "@/lib/audit-types";

import { AuditPager } from "./AuditPager";

const row = {} as AuditEntry;

function page(change: Partial<AuditPage> = {}): AuditPage {
  return {
    items: Array(25).fill(row),
    page: 1,
    size: 25,
    totalElements: 60,
    totalPages: 3,
    ...change,
  };
}

afterEach(() => {
  cleanup();
});

describe("AuditPager", () => {
  it("says which entries are shown and which page this is", () => {
    render(<AuditPager page={page()} busy={false} onPage={vi.fn()} />);

    expect(screen.getByRole("status").textContent).toBe("Showing 26–50 of 60");
    expect(screen.getByText("Page 2 of 3")).toBeTruthy();
  });

  it("goes to the next and previous page", () => {
    const onPage = vi.fn();

    render(<AuditPager page={page()} busy={false} onPage={onPage} />);

    fireEvent.click(screen.getByRole("button", { name: "Next" }));
    fireEvent.click(screen.getByRole("button", { name: "Previous" }));

    expect(onPage).toHaveBeenCalledWith(2);
    expect(onPage).toHaveBeenCalledWith(0);
  });

  it("cannot go before the first or after the last page", () => {
    const { rerender } = render(
      <AuditPager page={page({ page: 0 })} busy={false} onPage={vi.fn()} />,
    );

    expect(screen.getByRole("button", { name: "Previous" }).hasAttribute("disabled")).toBe(true);
    expect(screen.getByRole("button", { name: "Next" }).hasAttribute("disabled")).toBe(false);

    rerender(<AuditPager page={page({ page: 2 })} busy={false} onPage={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Next" }).hasAttribute("disabled")).toBe(true);
    expect(screen.getByRole("button", { name: "Previous" }).hasAttribute("disabled")).toBe(false);
  });

  it("switches the buttons off while a page is loading", () => {
    render(<AuditPager page={page()} busy onPage={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Previous" }).hasAttribute("disabled")).toBe(true);
    expect(screen.getByRole("button", { name: "Next" }).hasAttribute("disabled")).toBe(true);
  });

  it("has no buttons when everything fits on one page", () => {
    render(
      <AuditPager
        page={page({ page: 0, items: Array(7).fill(row), totalElements: 7, totalPages: 1 })}
        busy={false}
        onPage={vi.fn()}
      />,
    );

    expect(screen.getByRole("status").textContent).toBe("Showing 1–7 of 7");
    expect(screen.queryByRole("button", { name: "Next" })).toBeNull();
  });

  it("steps back to the last page when the current page is past the end", () => {
    const onPage = vi.fn();

    render(
      <AuditPager
        page={page({ page: 9, items: [], totalElements: 60, totalPages: 3 })}
        busy={false}
        onPage={onPage}
      />,
    );

    expect(screen.getByRole("status").textContent).toBe("No entries on this page (60 in total)");

    fireEvent.click(screen.getByRole("button", { name: "Previous" }));

    expect(onPage).toHaveBeenCalledWith(2);
  });
});
