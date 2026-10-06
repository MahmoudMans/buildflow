import { describe, expect, it } from "vitest";

import {
  buildProjectTimeline,
  dayNumber,
  dayToIso,
  summaryChips,
  todayIso,
  type TimelineOptions,
  type TimelineProject,
} from "./project-timeline";
import type { Milestone, TaskSummary } from "./types";

let counter = 0;

function task(overrides: Partial<TaskSummary> = {}): TaskSummary {
  counter += 1;

  return {
    id: `task-${counter}`,
    milestoneId: null,
    parentTaskId: null,
    title: `Task ${counter}`,
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

function milestone(overrides: Partial<Milestone> = {}): Milestone {
  counter += 1;

  return {
    id: `milestone-${counter}`,
    projectId: "project-1",
    name: `Milestone ${counter}`,
    description: null,
    plannedDate: null,
    completedDate: null,
    status: "UPCOMING",
    progressPercentage: 0,
    clientVisible: false,
    createdAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

const openProject: TimelineProject = {
  status: "ACTIVE",
  startDate: null,
  plannedEndDate: null,
};

function build(
  input: {
    project?: Partial<TimelineProject>;
    tasks?: TaskSummary[];
    milestones?: Milestone[];
  },
  options: Partial<TimelineOptions> = {},
) {
  return buildProjectTimeline({
    project: { ...openProject, ...input.project },
    tasks: input.tasks ?? [],
    milestones: input.milestones ?? [],
    options: { today: "2026-03-12", ...options },
  });
}

describe("date helpers", () => {
  it("turns real dates into day numbers and back", () => {
    const day = dayNumber("2026-02-28") as number;

    expect(dayToIso(day)).toBe("2026-02-28");
    expect(dayToIso(day + 1)).toBe("2026-03-01");
    expect(dayNumber("2026-03-02T10:30:00Z")).toBe(dayNumber("2026-03-02"));
  });

  it("rejects text that is not a real calendar date", () => {
    expect(dayNumber("2026-02-31")).toBeNull();
    expect(dayNumber("not a date")).toBeNull();
    expect(dayNumber("")).toBeNull();
    expect(dayNumber(null)).toBeNull();
  });

  it("reads today from the local calendar", () => {
    expect(todayIso(new Date(2026, 2, 5, 23, 59))).toBe("2026-03-05");
    expect(todayIso(new Date(2026, 11, 31, 0, 0))).toBe("2026-12-31");
  });
});

describe("buildProjectTimeline: the axis", () => {
  const first = task({
    title: "Foundations",
    startDate: "2026-03-02",
    dueDate: "2026-03-08",
  });
  const second = task({
    title: "Walls",
    startDate: "2026-03-09",
    dueDate: "2026-03-15",
  });

  it("runs from a Monday to a Sunday and places bars as shares of it", () => {
    const model = build({
      project: { startDate: "2026-03-02", plannedEndDate: "2026-03-29" },
      tasks: [first, second],
    });

    expect(model.rangeStart).toBe("2026-02-23");
    expect(model.rangeEnd).toBe("2026-04-05");
    expect(model.totalDays).toBe(42);
    expect(model.weekWidthPercent).toBeCloseTo(16.6667, 3);

    const [bars] = model.groups.map((group) => group.bars);

    expect(bars[0]).toMatchObject({
      title: "Foundations",
      leftPercent: 16.6667,
      widthPercent: 16.6667,
    });
    expect(bars[1]).toMatchObject({
      title: "Walls",
      leftPercent: 33.3333,
      widthPercent: 16.6667,
    });
    expect(model.projectBar).toMatchObject({
      startDate: "2026-03-02",
      endDate: "2026-03-29",
      leftPercent: 16.6667,
      widthPercent: 66.6667,
    });
  });

  it("labels a short schedule by week", () => {
    const model = build({
      project: { startDate: "2026-03-02", plannedEndDate: "2026-03-29" },
      tasks: [first],
    });

    expect(model.ticks.map((tick) => tick.label)).toEqual([
      "23 Feb",
      "2 Mar",
      "9 Mar",
      "16 Mar",
      "23 Mar",
      "30 Mar",
    ]);
    expect(model.ticks[0].leftPercent).toBe(0);
  });

  it("labels a long schedule by month", () => {
    const model = build({
      project: { startDate: "2026-01-15", plannedEndDate: "2026-09-30" },
    });

    expect(model.ticks.map((tick) => tick.label)).toEqual([
      "Jan 2026",
      "Feb 2026",
      "Mar 2026",
      "Apr 2026",
      "May 2026",
      "Jun 2026",
      "Jul 2026",
      "Aug 2026",
      "Sep 2026",
      "Oct 2026",
    ]);
    expect(model.ticks[0].leftPercent).toBe(0);
    expect(model.totalDays % 7).toBe(0);
  });

  it("never draws less than four weeks", () => {
    const model = build({
      tasks: [task({ startDate: "2026-03-04", dueDate: "2026-03-05" })],
    });

    expect(model.totalDays).toBe(28);
  });

  it("says when there is nothing to draw", () => {
    const model = build({
      tasks: [task({ title: "Order steel" })],
    });

    expect(model.hasSchedule).toBe(false);
    expect(model.groups).toEqual([]);
    expect(model.unscheduled.map((item) => item.title)).toEqual([
      "Order steel",
    ]);
  });
});

describe("buildProjectTimeline: today", () => {
  const tasks = [task({ startDate: "2026-03-02", dueDate: "2026-03-29" })];

  it("marks the middle of today when it is inside the range", () => {
    const model = build({ tasks }, { today: "2026-03-12" });

    // Range starts 2026-02-23 and lasts 42 days. 12 Mar is day 17, so its
    // middle is 17.5 days in.
    expect(model.today.position).toBe("within");
    expect(model.today.date).toBe("2026-03-12");
    expect(model.today.leftPercent).toBeCloseTo((17.5 / 42) * 100, 3);
  });

  it("does not stretch the chart to reach today", () => {
    const before = build({ tasks }, { today: "2026-01-01" });
    const after = build({ tasks }, { today: "2027-01-01" });
    const normal = build({ tasks });

    expect(before.today).toEqual({
      date: "2026-01-01",
      position: "before",
      leftPercent: null,
    });
    expect(after.today).toEqual({
      date: "2027-01-01",
      position: "after",
      leftPercent: null,
    });
    expect(before.totalDays).toBe(normal.totalDays);
    expect(after.totalDays).toBe(normal.totalDays);
  });
});

describe("buildProjectTimeline: task bars", () => {
  it("draws a task with one date as a single day", () => {
    const model = build({
      tasks: [task({ title: "Inspection", dueDate: "2026-03-10" })],
    });
    const bar = model.groups[0].bars[0];

    expect(bar.startDate).toBe("2026-03-10");
    expect(bar.endDate).toBe("2026-03-10");
    expect(bar.scheduleText).toBe("Due 10 Mar 2026");
    expect(bar.widthPercent).toBeCloseTo((1 / model.totalDays) * 100, 3);
  });

  it("describes a task that only has a start date", () => {
    const model = build({
      tasks: [task({ startDate: "2026-03-10" })],
    });

    expect(model.groups[0].bars[0].scheduleText).toBe("Starts 10 Mar 2026");
  });

  it("swaps dates that were entered the wrong way round", () => {
    const model = build({
      tasks: [task({ startDate: "2026-03-10", dueDate: "2026-03-04" })],
    });
    const bar = model.groups[0].bars[0];

    expect(bar.startDate).toBe("2026-03-04");
    expect(bar.endDate).toBe("2026-03-10");
  });

  it("lists tasks without usable dates instead of drawing them", () => {
    const stage = milestone({ name: "Structure" });
    const model = build({
      milestones: [stage],
      tasks: [
        task({ title: "Order steel", milestoneId: stage.id }),
        task({
          title: "Bad dates",
          startDate: "not a date",
          dueDate: "2026-02-31",
        }),
        task({ title: "Already done", status: "DONE" }),
        task({ title: "Dropped", status: "CANCELLED" }),
        task({ title: "Placed", startDate: "2026-03-02", dueDate: "2026-03-03" }),
      ],
    });

    expect(model.unscheduled).toEqual([
      {
        id: expect.any(String),
        title: "Order steel",
        status: "TODO",
        milestoneName: "Structure",
      },
      {
        id: expect.any(String),
        title: "Bad dates",
        status: "TODO",
        milestoneName: null,
      },
    ]);
    expect(
      model.groups.flatMap((group) => group.bars).map((bar) => bar.title),
    ).toEqual(["Placed"]);
    expect(model.summary.unscheduledTasks).toBe(2);
  });

  it("flags overdue and late-starting work, but never finished work", () => {
    const model = build(
      {
        tasks: [
          task({
            title: "Late",
            status: "IN_PROGRESS",
            startDate: "2026-03-01",
            dueDate: "2026-03-08",
            progressPercentage: 40,
          }),
          task({
            title: "Not started",
            status: "TODO",
            startDate: "2026-03-05",
            dueDate: "2026-03-20",
          }),
          task({
            title: "Finished",
            status: "DONE",
            startDate: "2026-02-20",
            dueDate: "2026-03-01",
            progressPercentage: 60,
          }),
          task({
            title: "Stuck",
            status: "BLOCKED",
            startDate: "2026-02-25",
            dueDate: "2026-03-01",
          }),
        ],
      },
      { today: "2026-03-12" },
    );
    const bars = Object.fromEntries(
      model.groups
        .flatMap((group) => group.bars)
        .map((bar) => [bar.title, bar]),
    );

    expect(bars["Late"]).toMatchObject({
      tone: "overdue",
      overdueDays: 4,
      progress: 40,
    });
    expect(bars["Late"].label).toContain("4 days overdue");
    expect(bars["Not started"]).toMatchObject({
      tone: "waiting",
      lateStart: true,
      overdueDays: 0,
    });
    expect(bars["Not started"].label).toContain("should have started");
    expect(bars["Finished"]).toMatchObject({
      tone: "done",
      overdueDays: 0,
      progress: 100,
    });
    expect(bars["Stuck"]).toMatchObject({ tone: "blocked", overdueDays: 11 });
  });

  it("names the people on a task", () => {
    const model = build({
      tasks: [
        task({
          startDate: "2026-03-02",
          dueDate: "2026-03-04",
          assignments: [
            {
              membershipId: "m1",
              userId: "u1",
              firstName: "Ada",
              lastName: "Byron",
              email: "ada@example.com",
              assignedAt: "2026-03-01T00:00:00Z",
            },
            {
              membershipId: "m2",
              userId: "u2",
              firstName: "",
              lastName: "",
              email: "sam@example.com",
              assignedAt: "2026-03-01T00:00:00Z",
            },
          ],
        }),
      ],
    });

    expect(model.groups[0].bars[0].assignees).toEqual([
      "Ada Byron",
      "sam@example.com",
    ]);
  });
});

describe("buildProjectTimeline: filters", () => {
  const tasks = [
    task({ title: "Open", status: "IN_PROGRESS", startDate: "2026-03-02", dueDate: "2026-03-20" }),
    task({ title: "Done", status: "DONE", startDate: "2026-03-02", dueDate: "2026-03-06" }),
    task({ title: "Dropped", status: "CANCELLED", startDate: "2026-03-02", dueDate: "2026-03-06" }),
  ];
  const titles = (model: ReturnType<typeof build>) =>
    model.groups.flatMap((group) => group.bars).map((bar) => bar.title);

  it("hides cancelled work by default", () => {
    expect(titles(build({ tasks }))).toEqual(["Done", "Open"]);
  });

  it("can hide finished work and show cancelled work", () => {
    expect(titles(build({ tasks }, { showCompleted: false }))).toEqual([
      "Open",
    ]);
    expect(titles(build({ tasks }, { showCancelled: true }))).toEqual([
      "Done",
      "Dropped",
      "Open",
    ]);

    const dropped = build({ tasks }, { showCancelled: true }).groups[0].bars.find(
      (bar) => bar.title === "Dropped",
    );

    expect(dropped?.tone).toBe("cancelled");
  });

  it("keeps the headline numbers about the whole project", () => {
    const all = build({ tasks });
    const filtered = build({ tasks }, { showCompleted: false });

    expect(filtered.summary).toEqual(all.summary);
    expect(all.summary.completedTasks).toBe(1);
    expect(all.summary.scheduledTasks).toBe(2);
  });
});

describe("buildProjectTimeline: milestones", () => {
  it("orders groups by date, puts undated ones next and loose tasks last", () => {
    const later = milestone({ name: "Handover", plannedDate: "2026-03-25" });
    const sooner = milestone({ name: "Frame", plannedDate: "2026-03-10" });
    const undated = milestone({ name: "Snagging" });
    const model = build({
      milestones: [later, undated, sooner],
      tasks: [task({ startDate: "2026-03-02", dueDate: "2026-03-04" })],
    });

    expect(model.groups.map((group) => group.title)).toEqual([
      "Frame",
      "Handover",
      "Snagging",
      "No milestone",
    ]);
    expect(model.groups[2].milestone?.leftPercent).toBeNull();
    expect(model.groups[2].milestone?.dateText).toBe("No date set");
    expect(model.groups[0].milestone?.leftPercent).toBeCloseTo(
      (((dayNumber("2026-03-10") as number) -
        (dayNumber(model.rangeStart) as number)) /
        model.totalDays) *
        100,
      3,
    );
  });

  it("nests sub-tasks under their parent", () => {
    const stage = milestone({ name: "Frame", plannedDate: "2026-03-20" });
    const parent = task({
      title: "Walls",
      milestoneId: stage.id,
      startDate: "2026-03-09",
      dueDate: "2026-03-20",
    });
    const child = task({
      title: "Ground floor walls",
      milestoneId: stage.id,
      parentTaskId: parent.id,
      startDate: "2026-03-09",
      dueDate: "2026-03-13",
    });
    const earlier = task({
      title: "Survey",
      milestoneId: stage.id,
      startDate: "2026-03-02",
      dueDate: "2026-03-03",
    });
    const model = build({
      milestones: [stage],
      tasks: [child, parent, earlier],
    });

    expect(
      model.groups[0].bars.map((bar) => `${bar.depth}:${bar.title}`),
    ).toEqual(["0:Survey", "0:Walls", "1:Ground floor walls"]);
  });

  it("treats a sub-task whose parent sits elsewhere as a top-level row", () => {
    const frame = milestone({ name: "Frame", plannedDate: "2026-03-20" });
    const parent = task({ title: "Roof", startDate: "2026-03-02", dueDate: "2026-03-04" });
    const child = task({
      title: "Tiles",
      milestoneId: frame.id,
      parentTaskId: parent.id,
      startDate: "2026-03-05",
      dueDate: "2026-03-06",
    });
    const model = build({ milestones: [frame], tasks: [parent, child] });

    expect(model.groups[0].bars.map((bar) => bar.depth)).toEqual([0]);
  });

  it("still shows tasks that point at each other", () => {
    const a = task({ id: "a", title: "A", parentTaskId: "b", startDate: "2026-03-02", dueDate: "2026-03-03" });
    const b = task({ id: "b", title: "B", parentTaskId: "a", startDate: "2026-03-04", dueDate: "2026-03-05" });
    const model = build({ tasks: [a, b] });

    expect(model.groups[0].bars.map((bar) => bar.title).sort()).toEqual(["A", "B"]);
  });

  it("hides cancelled milestones and keeps their tasks visible", () => {
    const dropped = milestone({ name: "Cancelled stage", status: "CANCELLED", plannedDate: "2026-03-10" });
    const model = build({
      milestones: [dropped],
      tasks: [task({ title: "Kept", milestoneId: dropped.id, startDate: "2026-03-02", dueDate: "2026-03-04" })],
    });

    expect(model.groups.map((group) => group.title)).toEqual(["No milestone"]);
    expect(model.groups[0].bars.map((bar) => bar.title)).toEqual(["Kept"]);
    expect(
      build({ milestones: [dropped], tasks: [] }, { showCancelled: true })
        .groups[0].milestone?.tone,
    ).toBe("cancelled");
  });

  it("colours a milestone by what has happened to it", () => {
    const tones = (status: Milestone["status"], plannedDate: string) =>
      build({ milestones: [milestone({ status, plannedDate })] }).groups[0]
        .milestone?.tone;

    expect(tones("COMPLETED", "2026-03-01")).toBe("done");
    expect(tones("MISSED", "2026-03-20")).toBe("overdue");
    expect(tones("UPCOMING", "2026-03-01")).toBe("overdue");
    expect(tones("UPCOMING", "2026-03-20")).toBe("waiting");
    expect(tones("IN_PROGRESS", "2026-03-20")).toBe("active");
  });

  it("shows the day a completed milestone was actually reached", () => {
    const model = build({
      milestones: [
        milestone({
          status: "COMPLETED",
          plannedDate: "2026-03-10",
          completedDate: "2026-03-08",
        }),
      ],
    });

    expect(model.groups[0].milestone?.dateText).toBe("08 Mar 2026");
  });
});

describe("buildProjectTimeline: summary", () => {
  it("counts what needs attention", () => {
    const model = build(
      {
        project: { startDate: "2026-03-01", plannedEndDate: "2026-03-31" },
        milestones: [
          milestone({ name: "Late one", plannedDate: "2026-03-05" }),
          milestone({ name: "Soon", plannedDate: "2026-03-14" }),
          milestone({ name: "Later", plannedDate: "2026-03-30" }),
          milestone({ name: "Finished", status: "COMPLETED", plannedDate: "2026-03-02" }),
        ],
        tasks: [
          task({ status: "IN_PROGRESS", startDate: "2026-03-01", dueDate: "2026-03-10" }),
          task({ status: "BLOCKED", startDate: "2026-03-05", dueDate: "2026-03-30" }),
          task({ status: "TODO", startDate: "2026-04-01", dueDate: "2026-04-10" }),
          task({ status: "DONE", startDate: "2026-03-01", dueDate: "2026-03-03" }),
          task({ status: "CANCELLED", startDate: "2026-03-01", dueDate: "2026-03-03" }),
          task({ status: "TODO" }),
        ],
      },
      { today: "2026-03-12" },
    );

    expect(model.summary).toMatchObject({
      scheduledTasks: 4,
      unscheduledTasks: 1,
      overdueTasks: 1,
      blockedTasks: 1,
      completedTasks: 1,
      tasksAfterProjectEnd: 1,
      overdueMilestones: 1,
      nextMilestone: { name: "Soon", plannedDate: "2026-03-14", daysUntil: 2 },
      projectEnd: { date: "2026-03-31", daysFromToday: 19 },
    });
  });

  it("stops talking about the planned end once the project is over", () => {
    const model = build({
      project: { status: "COMPLETED", startDate: "2026-03-01", plannedEndDate: "2026-03-31" },
      tasks: [task({ status: "TODO", startDate: "2026-04-01", dueDate: "2026-04-10" })],
    });

    expect(model.summary.projectEnd).toBeNull();
    expect(model.summary.tasksAfterProjectEnd).toBe(0);
  });

  it("turns the summary into short chips", () => {
    const model = build(
      {
        project: { startDate: "2026-03-01", plannedEndDate: "2026-03-10" },
        milestones: [milestone({ name: "Frame", plannedDate: "2026-03-13" })],
        tasks: [
          task({ status: "IN_PROGRESS", startDate: "2026-03-01", dueDate: "2026-03-05" }),
          task({ status: "BLOCKED", startDate: "2026-03-05", dueDate: "2026-03-30" }),
          task({ status: "TODO" }),
        ],
      },
      { today: "2026-03-12" },
    );
    const chips = summaryChips(model.summary);

    expect(chips.map((chip) => chip.text)).toEqual([
      "2 tasks scheduled",
      "1 overdue",
      "1 blocked",
      "1 without dates",
      "1 finish after the planned end",
      "Next milestone: Frame tomorrow",
      "Planned end 10 Mar 2026 (2 days late)",
    ]);
    expect(chips.find((chip) => chip.key === "overdue")?.tone).toBe("bad");
    expect(chips.find((chip) => chip.key === "blocked")?.tone).toBe("warn");
    expect(chips.find((chip) => chip.key === "project-end")?.tone).toBe("bad");
  });

  it("keeps the chips quiet when everything is fine", () => {
    const model = build({
      project: { startDate: "2026-03-01", plannedEndDate: "2026-04-30" },
      tasks: [task({ status: "IN_PROGRESS", startDate: "2026-03-10", dueDate: "2026-03-20" })],
    });

    expect(summaryChips(model.summary).map((chip) => chip.text)).toEqual([
      "1 task scheduled",
      "Planned end 30 Apr 2026 (in 49 days)",
    ]);
  });
});
