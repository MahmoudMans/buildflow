import { cleanup, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import { buildProjectTimeline } from "@/lib/project-timeline";
import type { Milestone, TaskSummary } from "@/lib/types";

import { ProjectTimelineView } from "./ProjectTimeline";

afterEach(() => {
  cleanup();
});

function task(overrides: Partial<TaskSummary> & { id: string }): TaskSummary {
  return {
    milestoneId: null,
    parentTaskId: null,
    title: overrides.id,
    status: "TODO",
    priority: "MEDIUM",
    startDate: null,
    dueDate: null,
    estimatedHours: null,
    actualHours: 0,
    progressPercentage: 0,
    weight: 1,
    assignments: [],
    ...overrides,
  };
}

const frame: Milestone = {
  id: "m-frame",
  projectId: "p1",
  name: "Frame complete",
  description: null,
  plannedDate: "2026-03-20",
  completedDate: null,
  status: "UPCOMING",
  progressPercentage: 0,
  clientVisible: false,
  createdAt: "2026-01-01T00:00:00Z",
};

function model(
  tasks: TaskSummary[],
  today = "2026-03-12",
  milestones: Milestone[] = [frame],
) {
  return buildProjectTimeline({
    project: {
      status: "ACTIVE",
      startDate: "2026-03-02",
      plannedEndDate: "2026-03-29",
    },
    tasks,
    milestones,
    options: { today },
  });
}

const walls = task({
  id: "t-walls",
  title: "Walls",
  milestoneId: "m-frame",
  status: "IN_PROGRESS",
  startDate: "2026-03-02",
  dueDate: "2026-03-08",
  progressPercentage: 40,
});
const survey = task({
  id: "t-survey",
  title: "Site survey",
  startDate: "2026-03-09",
  dueDate: "2026-03-15",
});

describe("ProjectTimelineView", () => {
  it("shows the summary, the milestone and a linked row for each task", () => {
    render(<ProjectTimelineView model={model([walls, survey])} projectId="p1" />);

    const chips = within(screen.getByRole("list", { name: "Schedule summary" }));

    expect(chips.getByText("2 tasks scheduled")).toBeTruthy();
    expect(chips.getByText("1 overdue")).toBeTruthy();

    expect(screen.getByText("Frame complete")).toBeTruthy();
    expect(screen.getByText("No milestone")).toBeTruthy();

    expect(
      screen.getByRole("link", { name: "Walls" }).getAttribute("href"),
    ).toBe("/app/projects/p1/tasks/t-walls");
    expect(
      screen.getByRole("link", { name: "Site survey" }).getAttribute("href"),
    ).toBe("/app/projects/p1/tasks/t-survey");

    expect(screen.getByText("4 days overdue")).toBeTruthy();
  });

  it("places each bar by its share of the axis and shows progress", () => {
    const { container } = render(
      <ProjectTimelineView model={model([walls])} projectId="p1" />,
    );

    const bar = container.querySelector<HTMLElement>(".uxTimelineBar");
    const fill = container.querySelector<HTMLElement>(".uxTimelineBarFill");

    // The axis starts on 2026-02-23 and lasts 42 days; the bar covers 7 days
    // starting 7 days in.
    expect(bar?.style.left).toBe("16.6667%");
    expect(bar?.style.width).toBe("16.6667%");
    expect(bar?.getAttribute("data-tone")).toBe("overdue");
    expect(fill?.style.width).toBe("40%");
  });

  it("keeps the duplicate bar link away from keyboard and screen readers", () => {
    const { container } = render(
      <ProjectTimelineView model={model([walls])} projectId="p1" />,
    );

    expect(screen.getAllByRole("link", { name: "Walls" })).toHaveLength(1);

    const bar = container.querySelector(".uxTimelineBar");

    expect(bar?.getAttribute("aria-hidden")).toBe("true");
    expect(bar?.getAttribute("tabindex")).toBe("-1");
  });

  it("draws a Today line only when today is on the chart", () => {
    const inside = render(
      <ProjectTimelineView model={model([walls])} projectId="p1" />,
    );

    expect(inside.container.querySelector(".uxTimelineToday")).toBeTruthy();

    cleanup();

    const outside = render(
      <ProjectTimelineView model={model([walls], "2027-01-01")} projectId="p1" />,
    );

    expect(outside.container.querySelector(".uxTimelineToday")).toBeNull();
    expect(screen.getByText(/is after the dates shown/)).toBeTruthy();
  });

  it("describes the milestone marker for assistive technology", () => {
    render(<ProjectTimelineView model={model([walls])} projectId="p1" />);

    expect(
      screen.getByRole("img", { name: /Frame complete.*Upcoming.*20 Mar 2026/ }),
    ).toBeTruthy();
  });

  it("says so when nothing can be drawn, and still lists the tasks", () => {
    render(
      <ProjectTimelineView
        model={buildProjectTimeline({
          project: { status: "ACTIVE", startDate: null, plannedEndDate: null },
          tasks: [
            task({
              id: "t-steel",
              title: "Order steel",
              milestoneId: "m-frame",
            }),
          ],
          milestones: [{ ...frame, plannedDate: null }],
          options: { today: "2026-03-12" },
        })}
        projectId="p1"
      />,
    );

    expect(screen.getByText(/Nothing is on the schedule yet/)).toBeTruthy();

    const list = within(
      screen.getByRole("region", { name: "Tasks without dates" }),
    );

    expect(
      list.getByRole("link", { name: "Order steel" }).getAttribute("href"),
    ).toBe("/app/projects/p1/tasks/t-steel");
    expect(list.getByText(/Frame complete/)).toBeTruthy();
  });
});
