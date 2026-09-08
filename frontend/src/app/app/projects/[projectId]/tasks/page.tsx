"use client";

import Link from "next/link";

import {
  FormEvent,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  useParams,
} from "next/navigation";

import { AppSidebar } from "@/components/AppSidebar";
import { ProjectWorkspaceNav } from "@/components/ProjectWorkspaceNav";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  Milestone,
  ProjectView,
  TaskPriority,
  TaskStatus,
  TaskSummary,
} from "@/lib/types";

type ViewMode =
  | "list"
  | "kanban"
  | "calendar";

const kanbanColumns: {
  status: TaskStatus;
  label: string;
}[] = [
  {
    status: "TODO",
    label: "Todo",
  },
  {
    status: "IN_PROGRESS",
    label: "In progress",
  },
  {
    status: "BLOCKED",
    label: "Blocked",
  },
  {
    status: "REVIEW",
    label: "Review",
  },
  {
    status: "DONE",
    label: "Done",
  },
];

export default function TasksPage() {
  const params = useParams();

  const projectId =
    String(params.projectId);

  const {
    user,
    organizations,
    loading,
  } = useAuth();

  const organizationSelection =
    useOrganizationSelection(
      organizations
    );

  const organizationId =
    organizationSelection.organizationId;

  const [project, setProject] =
    useState<ProjectView | null>(
      null
    );

  const [tasks, setTasks] =
    useState<TaskSummary[]>([]);

  const [milestones, setMilestones] =
    useState<Milestone[]>([]);

  const [view, setView] =
    useState<ViewMode>(
      "list"
    );

  const [title, setTitle] =
    useState("");

  const [priority, setPriority] =
    useState<TaskPriority>(
      "MEDIUM"
    );

  const [startDate, setStartDate] =
    useState("");

  const [dueDate, setDueDate] =
    useState("");

  const [weight, setWeight] =
    useState("1");

  const [estimatedHours, setEstimatedHours] =
    useState("");

  const [milestoneId, setMilestoneId] =
    useState("");

  const [parentTaskId, setParentTaskId] =
    useState("");

  const [error, setError] =
    useState("");



  async function loadData() {
    try {
      const [
        projectResult,
        taskResult,
        milestoneResult,
      ] = await Promise.all([
        apiGet<ProjectView>(
          `/projects/${projectId}`,
          organizationId
        ),

        apiGet<TaskSummary[]>(
          `/projects/${projectId}/tasks`,
          organizationId
        ),

        apiGet<Milestone[]>(
          `/projects/${projectId}/milestones`,
          organizationId
        ),
      ]);

      setProject(projectResult);
      setTasks(taskResult);
      setMilestones(milestoneResult);
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load project planning."
      );
    }
  }

  async function createTask(
    event: FormEvent
  ) {
    event.preventDefault();

    setError("");

    try {
      await apiPost(
        `/projects/${projectId}/tasks`,
        {
          title,
          priority,
          startDate:
            startDate || null,
          dueDate:
            dueDate || null,
          estimatedHours:
            estimatedHours
              ? Number(estimatedHours)
              : null,
          weight:
            Number(weight),
          milestoneId:
            milestoneId || null,
          parentTaskId:
            parentTaskId || null,
        },
        organizationId
      );

      setTitle("");
      setPriority("MEDIUM");
      setStartDate("");
      setDueDate("");
      setEstimatedHours("");
      setWeight("1");
      setMilestoneId("");
      setParentTaskId("");

      await loadData();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Task creation failed."
      );
    }
  }

  const rootTasks =
    tasks.filter(
      (task) =>
        task.parentTaskId === null
    );

  const calendarGroups =
    useMemo(() => {
      const map =
        new Map<
          string,
          TaskSummary[]
        >();

      tasks
        .filter(
          (task) =>
            task.dueDate !== null
        )
        .forEach(
          (task) => {
            const date =
              task.dueDate as string;

            const current =
              map.get(date) ?? [];

            current.push(task);

            map.set(
              date,
              current
            );
          }
        );

      return Array.from(
        map.entries()
      ).sort(
        ([left], [right]) =>
          left.localeCompare(
            right
          )
      );
    }, [tasks]);

    /* eslint-disable react-hooks/exhaustive-deps */
useEffect(() => {
    // BUILDFLOW_ASYNC_LOAD_EFFECT
    const timer =
      window.setTimeout(
        () => {
        if (
          organizationId
          && projectId
        ) {
          loadData();
        }
        },
        0
      );

    return () => {
      window.clearTimeout(
        timer
      );
    };
  }, [
    organizationId,
    projectId,
  ]);
  /* eslint-enable react-hooks/exhaustive-deps */

  if (
    loading
    || !user
    || !project
  ) {
    return (
      <main className="appLoading">
        {error || "Loading..."}
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>
              {project.projectNumber}
            </p>

            <h1>
              {project.name}
            </h1>

            <span className="projectClientName">
              Project planning
            </span>
          </div>

          <div className="planningProgress">
            <span>
              Project progress
            </span>

            <strong>
              {project.progressPercentage}%
            </strong>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="tasks"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="planningToolbar">
          <div className="viewSwitcher">
            <button
              type="button"
              className={
                view === "list"
                  ? "active"
                  : ""
              }
              onClick={() =>
                setView("list")
              }
            >
              List
            </button>

            <button
              type="button"
              className={
                view === "kanban"
                  ? "active"
                  : ""
              }
              onClick={() =>
                setView("kanban")
              }
            >
              Kanban
            </button>

            <button
              type="button"
              className={
                view === "calendar"
                  ? "active"
                  : ""
              }
              onClick={() =>
                setView("calendar")
              }
            >
              Calendar
            </button>
          </div>

          <span>
            {tasks.length} tasks
          </span>
        </section>

        <section className="planningLayout">
          <div>
            {view === "list" && (
              <div className="taskListView">
                {tasks.length === 0 && (
                  <div className="emptyState">
                    No tasks yet.
                  </div>
                )}

                {rootTasks.map(
                  (task) => {
                    const children =
                      tasks.filter(
                        (candidate) =>
                          candidate.parentTaskId
                            === task.id
                      );

                    return (
                      <div
                        className="taskGroup"
                        key={task.id}
                      >
                        <TaskRow
                          task={task}
                          projectId={projectId}
                        />

                        {children.map(
                          (child) => (
                            <div
                              className="subtaskRow"
                              key={child.id}
                            >
                              <TaskRow
                                task={child}
                                projectId={projectId}
                              />
                            </div>
                          )
                        )}
                      </div>
                    );
                  }
                )}
              </div>
            )}

            {view === "kanban" && (
              <div className="kanbanBoard">
                {kanbanColumns.map(
                  (column) => (
                    <section
                      className="kanbanColumn"
                      key={column.status}
                    >
                      <header>
                        <strong>
                          {column.label}
                        </strong>

                        <span>
                          {
                            tasks.filter(
                              (task) =>
                                task.status
                                  === column.status
                            ).length
                          }
                        </span>
                      </header>

                      <div>
                        {tasks
                          .filter(
                            (task) =>
                              task.status
                                === column.status
                          )
                          .map(
                            (task) => (
                              <Link
                                className="kanbanTask"
                                key={task.id}
                                href={`/app/projects/${projectId}/tasks/${task.id}`}
                              >
                                <span>
                                  {task.priority}
                                </span>

                                <strong>
                                  {task.title}
                                </strong>

                                <div>
                                  <small>
                                    {task.dueDate
                                      ?? "No due date"}
                                  </small>

                                  <small>
                                    {task.progressPercentage}%
                                  </small>
                                </div>
                              </Link>
                            )
                          )}
                      </div>
                    </section>
                  )
                )}
              </div>
            )}

            {view === "calendar" && (
              <div className="calendarTaskView">
                {calendarGroups.length ===
                  0 && (
                  <div className="emptyState">
                    No scheduled task deadlines.
                  </div>
                )}

                {calendarGroups.map(
                  ([date, dateTasks]) => (
                    <section
                      key={date}
                      className="calendarDay"
                    >
                      <header>
                        <strong>
                          {date}
                        </strong>

                        <span>
                          {dateTasks.length} tasks
                        </span>
                      </header>

                      <div>
                        {dateTasks.map(
                          (task) => (
                            <TaskRow
                              key={task.id}
                              task={task}
                              projectId={projectId}
                            />
                          )
                        )}
                      </div>
                    </section>
                  )
                )}
              </div>
            )}
          </div>

          <aside className="moduleFormCard">
            <h2>
              Create task
            </h2>

            <form
              className="authForm"
              onSubmit={createTask}
            >
              <label>
                Task title
                <input
                  required
                  value={title}
                  onChange={(event) =>
                    setTitle(
                      event.target.value
                    )
                  }
                />
              </label>

              <label>
                Priority
                <select
                  value={priority}
                  onChange={(event) =>
                    setPriority(
                      event.target
                        .value as TaskPriority
                    )
                  }
                >
                  <option value="LOW">
                    Low
                  </option>

                  <option value="MEDIUM">
                    Medium
                  </option>

                  <option value="HIGH">
                    High
                  </option>

                  <option value="CRITICAL">
                    Critical
                  </option>
                </select>
              </label>

              <label>
                Milestone
                <select
                  value={milestoneId}
                  onChange={(event) =>
                    setMilestoneId(
                      event.target.value
                    )
                  }
                >
                  <option value="">
                    No milestone
                  </option>

                  {milestones.map(
                    (milestone) => (
                      <option
                        key={milestone.id}
                        value={milestone.id}
                      >
                        {milestone.name}
                      </option>
                    )
                  )}
                </select>
              </label>

              <label>
                Parent task
                <select
                  value={parentTaskId}
                  onChange={(event) =>
                    setParentTaskId(
                      event.target.value
                    )
                  }
                >
                  <option value="">
                    Top-level task
                  </option>

                  {rootTasks.map(
                    (task) => (
                      <option
                        key={task.id}
                        value={task.id}
                      >
                        {task.title}
                      </option>
                    )
                  )}
                </select>
              </label>

              <div className="fieldRow">
                <label>
                  Start
                  <input
                    type="date"
                    value={startDate}
                    onChange={(event) =>
                      setStartDate(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Due
                  <input
                    type="date"
                    value={dueDate}
                    onChange={(event) =>
                      setDueDate(
                        event.target.value
                      )
                    }
                  />
                </label>
              </div>

              <div className="fieldRow">
                <label>
                  Estimated hours
                  <input
                    type="number"
                    min="0"
                    step="0.25"
                    value={estimatedHours}
                    onChange={(event) =>
                      setEstimatedHours(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Weight
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={weight}
                    onChange={(event) =>
                      setWeight(
                        event.target.value
                      )
                    }
                  />
                </label>
              </div>

              <button
                className="authSubmit"
                type="submit"
              >
                Create task
              </button>
            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}

function TaskRow({
  task,
  projectId,
}: {
  task: TaskSummary;
  projectId: string;
}) {
  return (
    <Link
      className="taskListRow"
      href={`/app/projects/${projectId}/tasks/${task.id}`}
    >
      <div>
        <strong>
          {task.title}
        </strong>

        <span>
          {task.assignments.length === 0
            ? "Unassigned"
            : task.assignments
                .map(
                  (assignment) =>
                    assignment.firstName
                )
                .join(", ")}
        </span>
      </div>

      <span className="statusBadge">
        {task.status}
      </span>

      <span>
        {task.priority}
      </span>

      <span>
        {task.dueDate ?? "—"}
      </span>

      <strong>
        {task.progressPercentage}%
      </strong>
    </Link>
  );
}
