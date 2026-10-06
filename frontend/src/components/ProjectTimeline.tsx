import Link from "next/link";
import { type CSSProperties, type ReactNode, useEffect, useRef } from "react";

import { formatDate } from "@/lib/format";
import {
  type TimelineBar,
  type TimelineGroup,
  type TimelineModel,
  type TimelineTone,
  summaryChips,
} from "@/lib/project-timeline";
import { humanizeStatus } from "@/lib/ux-validation";

const LEGEND: { tone: TimelineTone; label: string }[] = [
  { tone: "waiting", label: "Not started" },
  { tone: "active", label: "In progress" },
  { tone: "blocked", label: "Blocked" },
  { tone: "overdue", label: "Overdue" },
  { tone: "done", label: "Done" },
  { tone: "cancelled", label: "Cancelled" },
];

function taskHref(projectId: string, taskId: string): string {
  return `/app/projects/${projectId}/tasks/${taskId}`;
}

function peopleText(names: string[]): string {
  if (names.length <= 2) {
    return names.join(", ");
  }

  return `${names.slice(0, 2).join(", ")} +${names.length - 2}`;
}

function TimelineTrack({
  model,
  children,
}: {
  model: TimelineModel;
  children?: ReactNode;
}) {
  // Week lines are drawn by the background, so rows stay light.
  const style: CSSProperties = {
    backgroundSize: `${model.weekWidthPercent}% 100%`,
  };

  return (
    <div className="uxTimelineTrack" style={style}>
      {children}
    </div>
  );
}

function BarRow({
  bar,
  model,
  projectId,
}: {
  bar: TimelineBar;
  model: TimelineModel;
  projectId: string;
}) {
  const href = taskHref(projectId, bar.id);
  const details = [
    humanizeStatus(bar.status),
    bar.scheduleText,
    peopleText(bar.assignees),
  ].filter(Boolean);

  return (
    <div className="uxTimelineRow" role="listitem">
      <div
        className="uxTimelineLabel"
        style={{ paddingLeft: 12 + bar.depth * 16 }}
      >
        <Link href={href} className="uxTimelineName">
          {bar.title}
        </Link>

        <small>{details.join(" · ")}</small>

        {(bar.overdueDays > 0 || bar.lateStart) && (
          <span
            className="uxTimelineFlag"
            data-tone={bar.overdueDays > 0 ? "bad" : "warn"}
          >
            {bar.overdueDays > 0
              ? `${bar.overdueDays} ${bar.overdueDays === 1 ? "day" : "days"} overdue`
              : "Should have started"}
          </span>
        )}
      </div>

      <TimelineTrack model={model}>
        {/* The name link already goes to the task, so this copy is skipped
            by keyboard and screen readers. The title gives mouse users a hint. */}
        <Link
          href={href}
          className="uxTimelineBar"
          data-tone={bar.tone}
          title={bar.label}
          aria-hidden="true"
          tabIndex={-1}
          style={{
            left: `${bar.leftPercent}%`,
            width: `${bar.widthPercent}%`,
          }}
        >
          <span
            className="uxTimelineBarFill"
            style={{ width: `${bar.progress}%` }}
          />
        </Link>
      </TimelineTrack>
    </div>
  );
}

function GroupRows({
  group,
  model,
  projectId,
}: {
  group: TimelineGroup;
  model: TimelineModel;
  projectId: string;
}) {
  const { milestone } = group;

  return (
    <>
      <div
        className="uxTimelineRow uxTimelineMilestone"
        role="listitem"
        data-tone={milestone?.tone}
      >
        <div className="uxTimelineLabel">
          <strong>{group.title}</strong>

          {milestone && (
            <small>
              {humanizeStatus(milestone.status)} · {milestone.dateText}
            </small>
          )}
        </div>

        <TimelineTrack model={model}>
          {milestone && milestone.leftPercent !== null && (
            <span
              className="uxTimelineDiamond"
              data-tone={milestone.tone}
              title={milestone.label}
              role="img"
              aria-label={milestone.label}
              style={{ left: `${milestone.leftPercent}%` }}
            />
          )}
        </TimelineTrack>
      </div>

      {group.bars.map((bar) => (
        <BarRow key={bar.id} bar={bar} model={model} projectId={projectId} />
      ))}
    </>
  );
}

export function ProjectTimelineView({
  model,
  projectId,
}: {
  model: TimelineModel;
  projectId: string;
}) {
  const chips = summaryChips(model.summary);
  const scrollRef = useRef<HTMLDivElement>(null);
  const todayPercent = model.today.leftPercent;

  // On a narrow screen the chart is wider than the view. Start near today
  // instead of at the first week of the project.
  useEffect(() => {
    const scroller = scrollRef.current;

    if (!scroller || todayPercent === null) {
      return;
    }

    const labelWidth =
      scroller.querySelector(".uxTimelineLabel")?.getBoundingClientRect()
        .width ?? 0;
    const trackWidth = scroller.scrollWidth - labelWidth;
    const todayX = (todayPercent / 100) * trackWidth;
    const visibleTrack = scroller.clientWidth - labelWidth;

    scroller.scrollLeft = Math.max(0, todayX - visibleTrack * 0.35);
  }, [model.rangeStart, model.rangeEnd, todayPercent]);

  return (
    <section className="uxTimeline" aria-label="Project timeline">
      <ul className="uxTimelineChips" aria-label="Schedule summary">
        {chips.map((chip) => (
          <li key={chip.key} data-tone={chip.tone}>
            {chip.text}
          </li>
        ))}
      </ul>

      {!model.hasSchedule && (
        <p className="uxTimelineEmpty">
          Nothing is on the schedule yet. Give tasks a start or due date, or
          milestones a planned date, and they will appear here.
        </p>
      )}

      {model.hasSchedule && (
        <>
          {model.today.position !== "within" && (
            <p className="uxTimelineNote">
              Today ({formatDate(model.today.date)}) is{" "}
              {model.today.position === "before" ? "before" : "after"} the
              dates shown, so there is no Today line.
            </p>
          )}

          <div
            className="uxTimelineScroll"
            ref={scrollRef}
            role="region"
            aria-label="Schedule chart. Scroll sideways to see more."
            tabIndex={0}
          >
            <div
              className="uxTimelineCanvas"
              style={{
                minWidth: `calc(var(--uxTimelineLabel) + ${model.trackMinWidth}px)`,
              }}
            >
              <div className="uxTimelineRow uxTimelineAxisRow" aria-hidden="true">
                <div className="uxTimelineLabel" />

                <div className="uxTimelineTrack uxTimelineAxis">
                  {model.ticks.map((tick) => (
                    <span
                      key={tick.key}
                      className="uxTimelineTick"
                      style={{ left: `${tick.leftPercent}%` }}
                    >
                      {tick.label}
                    </span>
                  ))}
                </div>
              </div>

              <div className="uxTimelineBody" role="list">
                {model.today.leftPercent !== null && (
                  <div className="uxTimelineTodayLayer" aria-hidden="true">
                    <span
                      className="uxTimelineToday"
                      style={{ left: `${model.today.leftPercent}%` }}
                    >
                      <em>Today</em>
                    </span>
                  </div>
                )}

                {model.projectBar && (
                  <div
                    className="uxTimelineRow uxTimelineProjectRow"
                    role="listitem"
                  >
                    <div className="uxTimelineLabel">
                      <strong>Project schedule</strong>

                      <small>
                        {formatDate(model.projectBar.startDate)} –{" "}
                        {formatDate(model.projectBar.endDate)}
                      </small>
                    </div>

                    <TimelineTrack model={model}>
                      <span
                        className="uxTimelineProjectBar"
                        style={{
                          left: `${model.projectBar.leftPercent}%`,
                          width: `${model.projectBar.widthPercent}%`,
                        }}
                      />
                    </TimelineTrack>
                  </div>
                )}

                {model.groups.map((group) => (
                  <GroupRows
                    key={group.key}
                    group={group}
                    model={model}
                    projectId={projectId}
                  />
                ))}
              </div>
            </div>
          </div>

          <ul className="uxTimelineLegend" aria-label="Colour key">
            {LEGEND.map((item) => (
              <li key={item.tone}>
                <span
                  className="uxTimelineSwatch"
                  data-tone={item.tone}
                  aria-hidden="true"
                />
                {item.label}
              </li>
            ))}

            <li>
              <span
                className="uxTimelineSwatch uxTimelineSwatchDiamond"
                aria-hidden="true"
              />
              Milestone
            </li>
          </ul>
        </>
      )}

      {model.unscheduled.length > 0 && (
        <section
          className="uxTimelineUnscheduled"
          aria-label="Tasks without dates"
        >
          <h3>Not on the chart yet</h3>

          <p>These tasks have no start or due date, so they cannot be placed.</p>

          <ul>
            {model.unscheduled.map((item) => (
              <li key={item.id}>
                <Link href={taskHref(projectId, item.id)}>{item.title}</Link>

                <span>
                  {humanizeStatus(item.status)}
                  {item.milestoneName ? ` · ${item.milestoneName}` : ""}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </section>
  );
}
