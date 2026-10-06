import { formatDate } from "./format";
import type {
  Milestone,
  MilestoneStatus,
  ProjectStatus,
  TaskStatus,
  TaskSummary,
} from "./types";
import { humanizeStatus } from "./ux-validation";

const DAY_MS = 86_400_000;
const MONTH_NAMES = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];
const WEEKS_FOR_WEEKLY_TICKS = 12;
const MIN_RANGE_DAYS = 28;
const PIXELS_PER_DAY = 10;
const MIN_TRACK_PIXELS = 720;
const MAX_TRACK_PIXELS = 4000;

export type TimelineTone =
  | "done"
  | "active"
  | "waiting"
  | "blocked"
  | "overdue"
  | "cancelled";

export type TimelineProject = {
  status: ProjectStatus;
  startDate: string | null;
  plannedEndDate: string | null;
};

export type TimelineOptions = {
  /** Today as YYYY-MM-DD. The caller decides, so the result is repeatable. */
  today: string;
  showCompleted?: boolean;
  showCancelled?: boolean;
};

export type TimelineBar = {
  id: string;
  title: string;
  status: TaskStatus;
  tone: TimelineTone;
  startDate: string;
  endDate: string;
  scheduleText: string;
  leftPercent: number;
  widthPercent: number;
  progress: number;
  depth: 0 | 1 | 2;
  assignees: string[];
  overdueDays: number;
  lateStart: boolean;
  label: string;
};

export type TimelineMarker = {
  id: string;
  name: string;
  status: MilestoneStatus;
  tone: TimelineTone;
  plannedDate: string | null;
  completedDate: string | null;
  /** Null when the milestone has no planned date and cannot be placed. */
  leftPercent: number | null;
  dateText: string;
  label: string;
};

export type TimelineGroup = {
  key: string;
  milestone: TimelineMarker | null;
  title: string;
  bars: TimelineBar[];
};

export type TimelineTick = {
  key: string;
  label: string;
  leftPercent: number;
};

export type UnscheduledTask = {
  id: string;
  title: string;
  status: TaskStatus;
  milestoneName: string | null;
};

export type TimelineSummary = {
  scheduledTasks: number;
  unscheduledTasks: number;
  overdueTasks: number;
  blockedTasks: number;
  completedTasks: number;
  tasksAfterProjectEnd: number;
  overdueMilestones: number;
  nextMilestone: {
    name: string;
    plannedDate: string;
    daysUntil: number;
  } | null;
  projectEnd: {
    date: string;
    daysFromToday: number;
  } | null;
};

export type TimelineModel = {
  /** False when nothing can be placed on the chart. */
  hasSchedule: boolean;
  rangeStart: string;
  rangeEnd: string;
  totalDays: number;
  trackMinWidth: number;
  weekWidthPercent: number;
  ticks: TimelineTick[];
  today: {
    date: string;
    position: "before" | "within" | "after";
    leftPercent: number | null;
  };
  projectBar: {
    startDate: string;
    endDate: string;
    leftPercent: number;
    widthPercent: number;
  } | null;
  groups: TimelineGroup[];
  unscheduled: UnscheduledTask[];
  summary: TimelineSummary;
};

export type SummaryChip = {
  key: string;
  text: string;
  tone: "neutral" | "warn" | "bad";
};

/** YYYY-MM-DD from a local clock. */
export function todayIso(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${now.getFullYear()}-${month}-${day}`;
}

/** Whole days since 1970-01-01, or null if the text is not a real date. */
export function dayNumber(value: string | null | undefined): number | null {
  if (!value) {
    return null;
  }

  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);

  if (!match) {
    return null;
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const time = Date.UTC(year, month - 1, day);
  const check = new Date(time);

  if (check.getUTCMonth() !== month - 1 || check.getUTCDate() !== day) {
    return null;
  }

  return Math.round(time / DAY_MS);
}

export function dayToIso(day: number): string {
  return new Date(day * DAY_MS).toISOString().slice(0, 10);
}

/** 0 for Monday through 6 for Sunday. */
function mondayIndex(day: number): number {
  return (((day + 3) % 7) + 7) % 7;
}

// Fixed month names keep the axis the same on every browser and ICU version.
function shortDate(day: number): string {
  const date = new Date(day * DAY_MS);

  return `${date.getUTCDate()} ${MONTH_NAMES[date.getUTCMonth()]}`;
}

function monthLabel(day: number): string {
  const date = new Date(day * DAY_MS);

  return `${MONTH_NAMES[date.getUTCMonth()]} ${date.getUTCFullYear()}`;
}

function plural(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? "" : "s"}`;
}

function percent(value: number): number {
  return Math.round(value * 10_000) / 10_000;
}

function clampProgress(value: number | null | undefined): number {
  if (typeof value !== "number" || Number.isNaN(value)) {
    return 0;
  }

  return Math.min(100, Math.max(0, Math.round(value)));
}

function isClosedTask(status: TaskStatus): boolean {
  return status === "DONE" || status === "CANCELLED";
}

function taskTone(status: TaskStatus, overdue: boolean): TimelineTone {
  if (status === "DONE") {
    return "done";
  }

  if (status === "CANCELLED") {
    return "cancelled";
  }

  if (status === "BLOCKED") {
    return "blocked";
  }

  if (overdue) {
    return "overdue";
  }

  return status === "TODO" ? "waiting" : "active";
}

function milestoneTone(
  status: MilestoneStatus,
  plannedDay: number | null,
  today: number,
): TimelineTone {
  if (status === "COMPLETED") {
    return "done";
  }

  if (status === "CANCELLED") {
    return "cancelled";
  }

  if (status === "MISSED") {
    return "overdue";
  }

  if (plannedDay !== null && plannedDay < today) {
    return "overdue";
  }

  return status === "IN_PROGRESS" ? "active" : "waiting";
}

function personName(person: {
  firstName: string;
  lastName: string;
  email: string;
}): string {
  const name = `${person.firstName} ${person.lastName}`.trim();

  return name || person.email;
}

type Placed = {
  task: TaskSummary;
  startDay: number;
  endDay: number;
  hasBothDates: boolean;
};

function place(task: TaskSummary): Placed | null {
  const known = [dayNumber(task.startDate), dayNumber(task.dueDate)].filter(
    (value): value is number => value !== null,
  );

  if (known.length === 0) {
    return null;
  }

  return {
    task,
    startDay: Math.min(...known),
    endDay: Math.max(...known),
    hasBothDates: known.length === 2,
  };
}

function compareDay(left: number | null, right: number | null): number {
  if (left === null && right === null) {
    return 0;
  }

  if (left === null) {
    return 1;
  }

  if (right === null) {
    return -1;
  }

  return left - right;
}

function orderWithinGroup(placed: Placed[]): { item: Placed; depth: 0 | 1 | 2 }[] {
  const byStart = (left: Placed, right: Placed) =>
    left.startDay - right.startDay ||
    left.endDay - right.endDay ||
    left.task.title.localeCompare(right.task.title);

  const inGroup = new Map(placed.map((item) => [item.task.id, item]));
  const children = new Map<string, Placed[]>();
  const roots: Placed[] = [];

  for (const item of placed) {
    const parent = item.task.parentTaskId;

    if (parent && parent !== item.task.id && inGroup.has(parent)) {
      const list = children.get(parent) ?? [];

      list.push(item);
      children.set(parent, list);
    } else {
      roots.push(item);
    }
  }

  const ordered: { item: Placed; depth: 0 | 1 | 2 }[] = [];
  const seen = new Set<string>();

  const visit = (item: Placed, depth: number) => {
    if (seen.has(item.task.id)) {
      return;
    }

    seen.add(item.task.id);
    ordered.push({ item, depth: Math.min(depth, 2) as 0 | 1 | 2 });

    for (const child of (children.get(item.task.id) ?? []).sort(byStart)) {
      visit(child, depth + 1);
    }
  };

  for (const root of roots.sort(byStart)) {
    visit(root, 0);
  }

  // Tasks that only point at each other have no root. Show them anyway.
  for (const item of [...placed].sort(byStart)) {
    visit(item, 0);
  }

  return ordered;
}

export function buildProjectTimeline(input: {
  project: TimelineProject;
  tasks: TaskSummary[];
  milestones: Milestone[];
  options: TimelineOptions;
}): TimelineModel {
  const { project, tasks, milestones, options } = input;
  const todayDay = dayNumber(options.today) ?? dayNumber(todayIso()) ?? 0;
  const showCompleted = options.showCompleted ?? true;
  const showCancelled = options.showCancelled ?? false;

  const milestoneNames = new Map(
    milestones.map((milestone) => [milestone.id, milestone.name]),
  );

  // ----- summary: always about the whole project, whatever is filtered -----
  const countable = tasks.filter((task) => task.status !== "CANCELLED");
  const countablePlaced = countable.map(place);
  const projectStart = dayNumber(project.startDate);
  const projectEnd = dayNumber(project.plannedEndDate);
  const projectIsOpen =
    project.status !== "COMPLETED" &&
    project.status !== "CANCELLED" &&
    project.status !== "ARCHIVED";

  let overdueTasks = 0;
  let blockedTasks = 0;
  let completedTasks = 0;
  let tasksAfterProjectEnd = 0;
  let scheduledTasks = 0;

  countable.forEach((task, index) => {
    const placed = countablePlaced[index];

    if (task.status === "DONE") {
      completedTasks += 1;
    }

    if (task.status === "BLOCKED") {
      blockedTasks += 1;
    }

    if (!placed) {
      return;
    }

    scheduledTasks += 1;

    if (!isClosedTask(task.status) && placed.endDay < todayDay) {
      overdueTasks += 1;
    }

    if (
      projectIsOpen &&
      projectEnd !== null &&
      !isClosedTask(task.status) &&
      placed.endDay > projectEnd
    ) {
      tasksAfterProjectEnd += 1;
    }
  });

  const openMilestones = milestones.filter(
    (milestone) =>
      milestone.status !== "COMPLETED" && milestone.status !== "CANCELLED",
  );

  let overdueMilestones = 0;
  let nextMilestone: TimelineSummary["nextMilestone"] = null;
  let nextMilestoneDay: number | null = null;

  for (const milestone of openMilestones) {
    const day = dayNumber(milestone.plannedDate);

    if (milestone.status === "MISSED" || (day !== null && day < todayDay)) {
      overdueMilestones += 1;
      continue;
    }

    if (day !== null && (nextMilestoneDay === null || day < nextMilestoneDay)) {
      nextMilestoneDay = day;
      nextMilestone = {
        name: milestone.name,
        plannedDate: dayToIso(day),
        daysUntil: day - todayDay,
      };
    }
  }

  const unscheduledCountable = countable.filter(
    (task, index) => !countablePlaced[index] && !isClosedTask(task.status),
  ).length;

  // ----- what is drawn -----
  const visibleTasks = tasks.filter((task) => {
    if (task.status === "CANCELLED") {
      return showCancelled;
    }

    if (task.status === "DONE") {
      return showCompleted;
    }

    return true;
  });

  const visibleMilestones = milestones.filter(
    (milestone) => showCancelled || milestone.status !== "CANCELLED",
  );

  const placedVisible: Placed[] = [];
  const unscheduled: UnscheduledTask[] = [];

  for (const task of visibleTasks) {
    const placed = place(task);

    if (placed) {
      placedVisible.push(placed);
    } else if (!isClosedTask(task.status)) {
      unscheduled.push({
        id: task.id,
        title: task.title,
        status: task.status,
        milestoneName: task.milestoneId
          ? (milestoneNames.get(task.milestoneId) ?? null)
          : null,
      });
    }
  }

  const days: number[] = [];

  for (const value of [projectStart, projectEnd]) {
    if (value !== null) {
      days.push(value);
    }
  }

  for (const item of placedVisible) {
    days.push(item.startDay, item.endDay);
  }

  for (const milestone of visibleMilestones) {
    for (const value of [milestone.plannedDate, milestone.completedDate]) {
      const day = dayNumber(value);

      if (day !== null) {
        days.push(day);
      }
    }
  }

  const summary: TimelineSummary = {
    scheduledTasks,
    unscheduledTasks: unscheduledCountable,
    overdueTasks,
    blockedTasks,
    completedTasks,
    tasksAfterProjectEnd,
    overdueMilestones,
    nextMilestone,
    projectEnd:
      projectIsOpen && projectEnd !== null
        ? {
            date: dayToIso(projectEnd),
            daysFromToday: projectEnd - todayDay,
          }
        : null,
  };

  if (days.length === 0) {
    return {
      hasSchedule: false,
      rangeStart: dayToIso(todayDay),
      rangeEnd: dayToIso(todayDay),
      totalDays: 0,
      trackMinWidth: MIN_TRACK_PIXELS,
      weekWidthPercent: 100,
      ticks: [],
      today: { date: dayToIso(todayDay), position: "within", leftPercent: null },
      projectBar: null,
      groups: [],
      unscheduled,
      summary,
    };
  }

  // ----- axis: whole weeks, Monday to Sunday -----
  const lowest = Math.min(...days);
  const highest = Math.max(...days);
  const padding = Math.max(2, Math.ceil((highest - lowest) * 0.03));

  let rangeStart = lowest - padding;
  let rangeEnd = highest + padding;

  rangeStart -= mondayIndex(rangeStart);
  rangeEnd += 6 - mondayIndex(rangeEnd);

  if (rangeEnd - rangeStart + 1 < MIN_RANGE_DAYS) {
    rangeEnd = rangeStart + MIN_RANGE_DAYS - 1;
  }

  const totalDays = rangeEnd - rangeStart + 1;
  const offset = (day: number) => percent(((day - rangeStart) / totalDays) * 100);
  const spanPercent = (from: number, to: number) =>
    percent(((to - from + 1) / totalDays) * 100);

  const ticks: TimelineTick[] = [];

  if (totalDays <= WEEKS_FOR_WEEKLY_TICKS * 7) {
    for (let day = rangeStart; day <= rangeEnd; day += 7) {
      ticks.push({
        key: dayToIso(day),
        label: shortDate(day),
        leftPercent: offset(day),
      });
    }
  } else {
    const firsts: number[] = [];

    for (let day = rangeStart; day <= rangeEnd; day += 1) {
      if (new Date(day * DAY_MS).getUTCDate() === 1) {
        firsts.push(day);
      }
    }

    // The partial month at the start only gets a label when there is room.
    if (firsts[0] !== rangeStart) {
      const next = firsts[0] ?? rangeEnd + 1;

      if (((next - rangeStart) / totalDays) * 100 >= 6) {
        ticks.push({
          key: dayToIso(rangeStart),
          label: monthLabel(rangeStart),
          leftPercent: 0,
        });
      }
    }

    for (const day of firsts) {
      ticks.push({
        key: dayToIso(day),
        label: monthLabel(day),
        leftPercent: offset(day),
      });
    }
  }

  const todayInside = todayDay >= rangeStart && todayDay <= rangeEnd;

  // ----- rows, grouped by milestone -----
  const dated = visibleMilestones
    .map((milestone) => ({
      milestone,
      day: dayNumber(milestone.plannedDate),
    }))
    .sort(
      (left, right) =>
        compareDay(left.day, right.day) ||
        left.milestone.name.localeCompare(right.milestone.name),
    );

  const visibleMilestoneIds = new Set(
    visibleMilestones.map((milestone) => milestone.id),
  );
  const placedByMilestone = new Map<string | null, Placed[]>();

  for (const item of placedVisible) {
    const id =
      item.task.milestoneId && visibleMilestoneIds.has(item.task.milestoneId)
        ? item.task.milestoneId
        : null;
    const list = placedByMilestone.get(id) ?? [];

    list.push(item);
    placedByMilestone.set(id, list);
  }

  const toBar = (
    item: Placed,
    depth: 0 | 1 | 2,
  ): TimelineBar => {
    const { task } = item;
    const open = !isClosedTask(task.status);
    const overdueDays = open ? Math.max(0, todayDay - item.endDay) : 0;
    const lateStart =
      task.status === "TODO" && overdueDays === 0 && item.startDay < todayDay;
    const progress =
      task.status === "DONE" ? 100 : clampProgress(task.progressPercentage);
    const startDate = dayToIso(item.startDay);
    const endDate = dayToIso(item.endDay);

    const scheduleText = item.hasBothDates
      ? `${formatDate(startDate)} – ${formatDate(endDate)}`
      : task.dueDate && dayNumber(task.dueDate) !== null
        ? `Due ${formatDate(endDate)}`
        : `Starts ${formatDate(startDate)}`;

    const notes = [
      scheduleText,
      `${progress}% complete`,
      humanizeStatus(task.status),
    ];

    if (overdueDays > 0) {
      notes.push(`${plural(overdueDays, "day")} overdue`);
    } else if (lateStart) {
      notes.push("should have started");
    }

    return {
      id: task.id,
      title: task.title,
      status: task.status,
      tone: taskTone(task.status, overdueDays > 0),
      startDate,
      endDate,
      scheduleText,
      leftPercent: offset(item.startDay),
      widthPercent: spanPercent(item.startDay, item.endDay),
      progress,
      depth,
      assignees: (task.assignments ?? []).map(personName),
      overdueDays,
      lateStart,
      label: `${task.title}: ${notes.join(", ")}`,
    };
  };

  const toMarker = (
    milestone: Milestone,
    day: number | null,
  ): TimelineMarker => {
    const tone = milestoneTone(milestone.status, day, todayDay);
    const completedDay = dayNumber(milestone.completedDate);

    // A finished milestone shows the day it was reached, not the plan.
    const dateText =
      milestone.status === "COMPLETED" && completedDay !== null
        ? formatDate(dayToIso(completedDay))
        : day !== null
          ? formatDate(dayToIso(day))
          : "No date set";

    return {
      id: milestone.id,
      name: milestone.name,
      status: milestone.status,
      tone,
      plannedDate: day === null ? null : dayToIso(day),
      completedDate: completedDay === null ? null : dayToIso(completedDay),
      leftPercent: day === null ? null : offset(day),
      dateText,
      label: `Milestone ${milestone.name}: ${humanizeStatus(
        milestone.status,
      )}, ${dateText}`,
    };
  };

  const groups: TimelineGroup[] = [];

  for (const { milestone, day } of dated) {
    const placed = placedByMilestone.get(milestone.id) ?? [];

    groups.push({
      key: milestone.id,
      milestone: toMarker(milestone, day),
      title: milestone.name,
      bars: orderWithinGroup(placed).map(({ item, depth }) => toBar(item, depth)),
    });
  }

  const loose = placedByMilestone.get(null) ?? [];

  if (loose.length > 0) {
    groups.push({
      key: "no-milestone",
      milestone: null,
      title: "No milestone",
      bars: orderWithinGroup(loose).map(({ item, depth }) => toBar(item, depth)),
    });
  }

  return {
    hasSchedule: groups.length > 0 || projectStart !== null || projectEnd !== null,
    rangeStart: dayToIso(rangeStart),
    rangeEnd: dayToIso(rangeEnd),
    totalDays,
    trackMinWidth: Math.min(
      MAX_TRACK_PIXELS,
      Math.max(MIN_TRACK_PIXELS, totalDays * PIXELS_PER_DAY),
    ),
    weekWidthPercent: percent((7 / totalDays) * 100),
    ticks,
    today: todayInside
      ? {
          date: dayToIso(todayDay),
          position: "within",
          leftPercent: offset(todayDay + 0.5),
        }
      : {
          date: dayToIso(todayDay),
          position: todayDay < rangeStart ? "before" : "after",
          leftPercent: null,
        },
    projectBar:
      projectStart !== null && projectEnd !== null && projectEnd >= projectStart
        ? {
            startDate: dayToIso(projectStart),
            endDate: dayToIso(projectEnd),
            leftPercent: offset(projectStart),
            widthPercent: spanPercent(projectStart, projectEnd),
          }
        : null,
    groups,
    unscheduled,
    summary,
  };
}

function daysPhrase(days: number): string {
  if (days === 0) {
    return "today";
  }

  if (days === 1) {
    return "tomorrow";
  }

  if (days === -1) {
    return "yesterday";
  }

  return days > 0 ? `in ${days} days` : `${-days} days ago`;
}

/** The short facts shown above the chart. */
export function summaryChips(summary: TimelineSummary): SummaryChip[] {
  const chips: SummaryChip[] = [
    {
      key: "scheduled",
      text: `${plural(summary.scheduledTasks, "task")} scheduled`,
      tone: "neutral",
    },
  ];

  if (summary.overdueTasks > 0) {
    chips.push({
      key: "overdue",
      text: `${summary.overdueTasks} overdue`,
      tone: "bad",
    });
  }

  if (summary.blockedTasks > 0) {
    chips.push({
      key: "blocked",
      text: `${summary.blockedTasks} blocked`,
      tone: "warn",
    });
  }

  if (summary.unscheduledTasks > 0) {
    chips.push({
      key: "unscheduled",
      text: `${summary.unscheduledTasks} without dates`,
      tone: "warn",
    });
  }

  if (summary.tasksAfterProjectEnd > 0) {
    chips.push({
      key: "past-end",
      text: `${summary.tasksAfterProjectEnd} finish after the planned end`,
      tone: "bad",
    });
  }

  if (summary.overdueMilestones > 0) {
    chips.push({
      key: "milestones-late",
      text: `${plural(summary.overdueMilestones, "milestone")} late`,
      tone: "bad",
    });
  }

  if (summary.nextMilestone) {
    chips.push({
      key: "next-milestone",
      text: `Next milestone: ${summary.nextMilestone.name} ${daysPhrase(
        summary.nextMilestone.daysUntil,
      )}`,
      tone: "neutral",
    });
  }

  if (summary.projectEnd) {
    const { date, daysFromToday } = summary.projectEnd;

    chips.push({
      key: "project-end",
      text: `Planned end ${formatDate(date)} (${
        daysFromToday < 0
          ? `${plural(-daysFromToday, "day")} late`
          : daysPhrase(daysFromToday)
      })`,
      tone: daysFromToday < 0 ? "bad" : "neutral",
    });
  }

  return chips;
}
