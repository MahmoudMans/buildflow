"use client";

import Link from "next/link";

import {
  FormEvent,
  useEffect,
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
  apiDelete,
  apiGet,
  apiPatch,
  apiPost,
} from "@/lib/api";

import type {
  ProjectMember,
  ProjectView,
  TaskComment,
  TaskStatus,
  TaskView,
  TaskWorkLog,
} from "@/lib/types";
import { formatDateTime } from "@/lib/format";

function actionsFor(
  status: TaskStatus
) {
  switch (status) {
    case "TODO":
      return [
        {
          label: "Start",
          path: "start",
        },
        {
          label: "Cancel",
          path: "cancel",
        },
      ];

    case "IN_PROGRESS":
      return [
        {
          label: "Block",
          path: "block",
        },
        {
          label: "Review",
          path: "review",
        },
        {
          label: "Complete",
          path: "complete",
        },
        {
          label: "Cancel",
          path: "cancel",
        },
      ];

    case "BLOCKED":
      return [
        {
          label: "Resume",
          path: "start",
        },
        {
          label: "Cancel",
          path: "cancel",
        },
      ];

    case "REVIEW":
      return [
        {
          label: "Resume",
          path: "start",
        },
        {
          label: "Complete",
          path: "complete",
        },
        {
          label: "Cancel",
          path: "cancel",
        },
      ];

    case "DONE":
    case "CANCELLED":
      return [
        {
          label: "Reopen",
          path: "reopen",
        },
      ];
  }
}

export default function TaskDetailPage() {
  const params = useParams();

  const projectId =
    String(params.projectId);

  const taskId =
    String(params.taskId);

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

  const [task, setTask] =
    useState<TaskView | null>(
      null
    );

  const [projectMembers, setProjectMembers] =
    useState<ProjectMember[]>([]);

  const [comments, setComments] =
    useState<TaskComment[]>([]);

  const [workLogs, setWorkLogs] =
    useState<TaskWorkLog[]>([]);

  const [membershipId, setMembershipId] =
    useState("");

  const [comment, setComment] =
    useState("");

  const [progress, setProgress] =
    useState("");

  const [workDate, setWorkDate] =
    useState(
      new Date()
        .toISOString()
        .slice(0, 10)
    );

  const [minutesWorked, setMinutesWorked] =
    useState("60");

  const [workDescription, setWorkDescription] =
    useState("");

  const [error, setError] =
    useState("");



  async function loadData() {
    try {
      const [
        projectResult,
        taskResult,
        memberResult,
        commentResult,
        workLogResult,
      ] = await Promise.all([
        apiGet<ProjectView>(
          `/projects/${projectId}`,
          organizationId
        ),

        apiGet<TaskView>(
          `/tasks/${taskId}`,
          organizationId
        ),

        apiGet<ProjectMember[]>(
          `/projects/${projectId}/members`,
          organizationId
        ),

        apiGet<TaskComment[]>(
          `/tasks/${taskId}/comments`,
          organizationId
        ),

        apiGet<TaskWorkLog[]>(
          `/tasks/${taskId}/work-logs`,
          organizationId
        ),
      ]);

      setProject(projectResult);
      setTask(taskResult);
      setProjectMembers(memberResult);
      setComments(commentResult);
      setWorkLogs(workLogResult);

      setProgress(
        String(
          taskResult.progressPercentage
        )
      );
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load task."
      );
    }
  }

  async function transition(
    path: string
  ) {
    try {
      await apiPost(
        `/tasks/${taskId}/${path}`,
        undefined,
        organizationId
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function updateProgress(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPatch(
        `/tasks/${taskId}/progress`,
        {
          progressPercentage:
            Number(progress),
        },
        organizationId
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function assign(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!membershipId) {
      return;
    }

    try {
      await apiPost(
        `/tasks/${taskId}/assignments`,
        {
          membershipId,
        },
        organizationId
      );

      setMembershipId("");

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function unassign(
    membership: string
  ) {
    try {
      await apiDelete(
        `/tasks/${taskId}/assignments/${membership}`,
        organizationId
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function addComment(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!comment.trim()) {
      return;
    }

    try {
      await apiPost(
        `/tasks/${taskId}/comments`,
        {
          content: comment,
        },
        organizationId
      );

      setComment("");

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function addWorkLog(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPost(
        `/tasks/${taskId}/work-logs`,
        {
          workDate,
          minutesWorked:
            Number(minutesWorked),
          description:
            workDescription || null,
        },
        organizationId
      );

      setMinutesWorked("60");
      setWorkDescription("");

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  function handleError(
    caught: unknown
  ) {
    setError(
      caught instanceof ApiClientError
        ? caught.message
        : "The operation failed."
    );
  }

    /* eslint-disable react-hooks/exhaustive-deps */
useEffect(() => {
    // BUILDFLOW_ASYNC_LOAD_EFFECT
    const timer =
      window.setTimeout(
        () => {
        if (
          organizationId
          && projectId
          && taskId
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
    taskId,
  ]);
  /* eslint-enable react-hooks/exhaustive-deps */

  if (
    loading
    || !user
    || !project
    || !task
  ) {
    return (
      <main className="appLoading">
        {error || "Loading..."}
      </main>
    );
  }

  const assignedIds =
    new Set(
      task.assignments.map(
        (assignment) =>
          assignment.membershipId
      )
    );

  const availableMembers =
    projectMembers.filter(
      (member) =>
        !assignedIds.has(
          member.membershipId
        )
    );

  return (
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent">
        <header className="appHeader projectDetailHeader">
          <div>
            <p>
              {project.projectNumber}
            </p>

            <h1>
              {task.title}
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>

          <div className="projectActions">
            {actionsFor(
              task.status
            ).map((action) => (
              <button
                type="button"
                className="smallButton"
                key={action.path}
                onClick={() =>
                  transition(
                    action.path
                  )
                }
              >
                {action.label}
              </button>
            ))}

            <Link
              className="smallButton"
              href={`/app/projects/${projectId}/tasks`}
            >
              Back
            </Link>
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

        <section className="detailCards projectMetrics">
          <article>
            <span>Status</span>
            <strong>
              {task.status}
            </strong>
          </article>

          <article>
            <span>Progress</span>
            <strong>
              {task.progressPercentage}%
            </strong>
          </article>

          <article>
            <span>Priority</span>
            <strong>
              {task.priority}
            </strong>
          </article>

          <article>
            <span>Weight</span>
            <strong>
              {task.weight}
            </strong>
          </article>

          <article>
            <span>Estimated</span>
            <strong>
              {task.estimatedHours ?? 0} h
            </strong>
          </article>

          <article>
            <span>Actual</span>
            <strong>
              {task.actualHours} h
            </strong>
          </article>
        </section>

        <section className="taskDetailGrid">
          <article className="taskDetailCard">
            <h2>
              Progress
            </h2>

            <div className="taskProgressTrack">
              <div
                style={{
                  width: `${task.progressPercentage}%`,
                }}
              />
            </div>

            <form
              className="inlineForm"
              onSubmit={updateProgress}
            >
              <input
                type="number"
                min="0"
                max="100"
                step="1"
                value={progress}
                onChange={(event) =>
                  setProgress(
                    event.target.value
                  )
                }
              />

              <button
                className="smallButton"
                type="submit"
              >
                Update progress
              </button>
            </form>
          </article>

          <article className="taskDetailCard">
            <h2>
              Schedule
            </h2>

            <dl className="detailDefinition">
              <div>
                <dt>Start</dt>
                <dd>
                  {task.startDate ?? "—"}
                </dd>
              </div>

              <div>
                <dt>Due</dt>
                <dd>
                  {task.dueDate ?? "—"}
                </dd>
              </div>
            </dl>
          </article>
        </section>

        <section className="moduleLayout">
          <div className="moduleTableCard">
            <div className="moduleCardHeader">
              <div>
                <h2>
                  Task assignees
                </h2>

                <p>
                  Members responsible for this task.
                </p>
              </div>
            </div>

            <div className="dataList">
              {task.assignments.length ===
                0 && (
                <div className="emptyState">
                  No assignees.
                </div>
              )}

              {task.assignments.map(
                (assignment) => (
                  <div
                    className="dataRow"
                    key={assignment.membershipId}
                  >
                    <div>
                      <strong>
                        {assignment.firstName}
                        {" "}
                        {assignment.lastName}
                      </strong>

                      <span>
                        {assignment.email}
                      </span>
                    </div>

                    <span>
                      Assigned
                    </span>

                    <span>
                      —
                    </span>

                    <button
                      className="smallButton"
                      type="button"
                      onClick={() =>
                        unassign(
                          assignment.membershipId
                        )
                      }
                    >
                      Remove
                    </button>
                  </div>
                )
              )}
            </div>
          </div>

          <aside className="moduleFormCard">
            <h2>
              Assign member
            </h2>

            <form
              className="authForm"
              onSubmit={assign}
            >
              <label>
                Project member
                <select
                  value={membershipId}
                  onChange={(event) =>
                    setMembershipId(
                      event.target.value
                    )
                  }
                >
                  <option value="">
                    Select member
                  </option>

                  {availableMembers.map(
                    (member) => (
                      <option
                        key={member.membershipId}
                        value={member.membershipId}
                      >
                        {member.firstName}
                        {" "}
                        {member.lastName}
                      </option>
                    )
                  )}
                </select>
              </label>

              <button
                className="authSubmit"
                type="submit"
                disabled={!membershipId}
              >
                Assign
              </button>
            </form>
          </aside>
        </section>

        <section className="taskCollaborationGrid">
          <article className="moduleTableCard">
            <div className="moduleCardHeader">
              <div>
                <h2>
                  Comments
                </h2>

                <p>
                  Task discussion and updates.
                </p>
              </div>
            </div>

            <div className="commentList">
              {comments.length === 0 && (
                <div className="emptyState">
                  No comments.
                </div>
              )}

              {comments.map(
                (item) => (
                  <div
                    className="commentItem"
                    key={item.id}
                  >
                    <header>
                      <strong>
                        {item.authorName}
                      </strong>

                      <span>
                        {formatDateTime(item.createdAt)}
                        {item.edited
                          ? " · Edited"
                          : ""}
                      </span>
                    </header>

                    <p>
                      {item.content}
                    </p>
                  </div>
                )
              )}
            </div>

            <form
              className="commentComposer"
              onSubmit={addComment}
            >
              <textarea
                required
                value={comment}
                onChange={(event) =>
                  setComment(
                    event.target.value
                  )
                }
                placeholder="Add a project update..."
              />

              <button
                className="smallButton"
                type="submit"
              >
                Add comment
              </button>
            </form>
          </article>

          <article className="moduleTableCard">
            <div className="moduleCardHeader">
              <div>
                <h2>
                  Work logs
                </h2>

                <p>
                  Time recorded against this task.
                </p>
              </div>
            </div>

            <div className="workLogList">
              {workLogs.map(
                (log) => (
                  <div
                    className="workLogItem"
                    key={log.id}
                  >
                    <div>
                      <strong>
                        {log.memberName}
                      </strong>

                      <span>
                        {log.workDate}
                      </span>
                    </div>

                    <strong>
                      {(
                        log.minutesWorked / 60
                      ).toFixed(2)}
                      {" h"}
                    </strong>

                    <p>
                      {log.description
                        ?? ""}
                    </p>
                  </div>
                )
              )}
            </div>

            <form
              className="workLogForm"
              onSubmit={addWorkLog}
            >
              <div className="fieldRow">
                <label>
                  Date
                  <input
                    type="date"
                    required
                    value={workDate}
                    onChange={(event) =>
                      setWorkDate(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Minutes
                  <input
                    type="number"
                    min="1"
                    max="1440"
                    required
                    value={minutesWorked}
                    onChange={(event) =>
                      setMinutesWorked(
                        event.target.value
                      )
                    }
                  />
                </label>
              </div>

              <label>
                Description
                <textarea
                  value={workDescription}
                  onChange={(event) =>
                    setWorkDescription(
                      event.target.value
                    )
                  }
                />
              </label>

              <button
                className="smallButton"
                type="submit"
              >
                Record work
              </button>
            </form>
          </article>
        </section>
      </section>
    </main>
  );
}
