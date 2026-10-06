import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { ChangeOrderImpact } from "@/lib/change-order-impact";

import { ChangeOrderImpactPanel } from "./ChangeOrderImpactPanel";

const mocks = vi.hoisted(() => {
  class FakeApiClientError extends Error {}

  return {
    apiGet: vi.fn(),
    FakeApiClientError,
  };
});

vi.mock("@/lib/api", () => ({
  apiGet: mocks.apiGet,
  ApiClientError: mocks.FakeApiClientError,
}));

function sample(overrides: Partial<ChangeOrderImpact> = {}): ChangeOrderImpact {
  return {
    changeOrderId: "co-1",
    projectId: "project-1",
    changeOrderNumber: "CO-2026-0001",
    title: "Extra waterproofing",
    status: "SUBMITTED",
    pending: true,
    alreadyApproved: false,
    costImpact: 10000,
    clientPriceImpact: 15000,
    scheduleImpactDays: 14,
    changeOrderProfit: 5000,
    changeOrderMarginPercentage: 33.3333,
    current: {
      budget: 85000,
      contractValue: 108000,
      plannedProfit: 23000,
      plannedMarginPercentage: 21.2963,
      forecastCost: 0,
      forecastProfit: 108000,
      forecastMarginPercentage: 100,
      budgetHeadroom: 85000,
      plannedEndDate: "2027-06-30",
    },
    afterApproval: {
      budget: 95000,
      contractValue: 123000,
      plannedProfit: 28000,
      plannedMarginPercentage: 22.7642,
      forecastCost: 10000,
      forecastProfit: 113000,
      forecastMarginPercentage: 91.8699,
      budgetHeadroom: 85000,
      plannedEndDate: "2027-07-14",
    },
    warnings: ["SCHEDULE_DELAY"],
    pipeline: {
      pendingCount: 2,
      pendingCostImpact: 13000,
      pendingClientPriceImpact: 17000,
    },
    ...overrides,
  };
}

beforeEach(() => {
  mocks.apiGet.mockReset();
});

afterEach(() => {
  cleanup();
});

describe("ChangeOrderImpactPanel", () => {
  it("loads the preview and shows the figures, warnings and notes", async () => {
    mocks.apiGet.mockResolvedValue(sample());

    render(
      <ChangeOrderImpactPanel
        changeOrderId="co-1"
        organizationId="org-1"
        currency="EUR"
        status="SUBMITTED"
      />,
    );

    expect(await screen.findByText("Planned profit")).toBeTruthy();

    expect(mocks.apiGet).toHaveBeenCalledWith(
      "/change-orders/co-1/impact",
      "org-1",
    );

    expect(screen.getByText("€95,000.00")).toBeTruthy();
    // Budget and forecast cost both rise by the cost impact.
    expect(screen.getAllByText("+€10,000.00")).toHaveLength(2);
    expect(screen.getByText("+14 days")).toBeTruthy();
    expect(screen.getByText("14 Jul 2027")).toBeTruthy();

    expect(
      screen.getByText("The planned end date would move later."),
    ).toBeTruthy();

    expect(
      screen.getByText("This change order: Earns €5,000.00 (33.3% margin)."),
    ).toBeTruthy();

    expect(
      screen.getByText(/2 change orders on this project are still pending/),
    ).toBeTruthy();
  });

  it("shows nothing and does not ask the server once it is decided", () => {
    const { container } = render(
      <ChangeOrderImpactPanel
        changeOrderId="co-1"
        organizationId="org-1"
        currency="EUR"
        status="APPROVED"
      />,
    );

    expect(container.textContent).toBe("");
    expect(mocks.apiGet).not.toHaveBeenCalled();
  });

  it("explains a failure and lets the user try again", async () => {
    mocks.apiGet.mockRejectedValueOnce(
      new mocks.FakeApiClientError("You cannot see the financials."),
    );
    mocks.apiGet.mockResolvedValueOnce(sample());

    render(
      <ChangeOrderImpactPanel
        changeOrderId="co-1"
        organizationId="org-1"
        currency="EUR"
        status="DRAFT"
      />,
    );

    expect(
      await screen.findByText("You cannot see the financials."),
    ).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByText("Planned profit")).toBeTruthy();
    expect(mocks.apiGet).toHaveBeenCalledTimes(2);
  });

  it("asks again when the change order changes", async () => {
    mocks.apiGet.mockResolvedValue(sample());

    const { rerender } = render(
      <ChangeOrderImpactPanel
        changeOrderId="co-1"
        organizationId="org-1"
        currency="EUR"
        status="SUBMITTED"
        version="v1"
      />,
    );

    await screen.findByText("Planned profit");

    mocks.apiGet.mockResolvedValue(
      sample({ changeOrderId: "co-2", changeOrderNumber: "CO-2026-0002" }),
    );

    rerender(
      <ChangeOrderImpactPanel
        changeOrderId="co-2"
        organizationId="org-1"
        currency="EUR"
        status="SUBMITTED"
        version="v1"
      />,
    );

    await waitFor(() => {
      expect(mocks.apiGet).toHaveBeenLastCalledWith(
        "/change-orders/co-2/impact",
        "org-1",
      );
    });

    expect(
      await screen.findByText(/if CO-2026-0002 is approved/),
    ).toBeTruthy();
  });
});
