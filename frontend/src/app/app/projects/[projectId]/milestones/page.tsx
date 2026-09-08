"use client";

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
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  Milestone,
  MilestoneStatus,
  ProjectView,
} from "@/lib/types";

function milestoneActions(
  status: MilestoneStatus
) {
  switch (status) {
    case "UPCOMING":
      return [
        {
          label: "Start",
          path: "start",
        },
        {
          label: "Complete",
          path: "complete",
        },
        {
          label: "Mark missed",
          path: "miss",
        },
        {
          label: "Cancel",
          path: "cancel",
        },
      ];

    case "IN_PROGRESS":
      return [
        {
          label: "Complete",
          path: "complete",
        },
        {
          label: "Mark missed",
          path: "miss",
        },
        {
          label: "Cancel",
          path: "cancel",
        },
      ];

    case "MISSED":
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

    default:
      return [];
  }
}

export default function MilestonesPage() {
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

  const [milestones, setMilestones] =
    useState<Milestone[]>([]);

  const [name, setName] =
    useState("");

  const [description, setDescription] =
    useState("");

  const [plannedDate, setPlannedDate] =
    useState("");

  const [clientVisible, setClientVisible] =
    useState(false);

  const [error, setError] =
    useState("");



  async function loadData() {
    try {
      const [
        projectResult,
        milestoneResult,
      ] = await Promise.all([
        apiGet<ProjectView>(
          `/projects/${projectId}`,
          organizationId
        ),

        apiGet<Milestone[]>(
          `/projects/${projectId}/milestones`,
          organizationId
        ),
      ]);

      setProject(projectResult);
      setMilestones(
        milestoneResult
      );
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load milestones."
      );
    }
  }

  async function createMilestone(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPost(
        `/projects/${projectId}/milestones`,
        {
          name,
          description:
            description || null,
          plannedDate:
            plannedDate || null,
          clientVisible,
        },
        organizationId
      );

      setName("");
      setDescription("");
      setPlannedDate("");
      setClientVisible(false);

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  async function transition(
    milestoneId: string,
    path: string
  ) {
    try {
      await apiPost(
        `/projects/${projectId}/milestones/${milestoneId}/${path}`,
        undefined,
        organizationId
      );

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
              Milestones
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="milestones"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="moduleLayout">
          <div className="milestoneTimeline">
            {milestones.length ===
              0 && (
              <div className="emptyState">
                No milestones yet.
              </div>
            )}

            {milestones.map(
              (milestone) => (
                <article
                  className="milestoneCard"
                  key={milestone.id}
                >
                  <div className="milestoneMarker" />

                  <div className="milestoneContent">
                    <header>
                      <div>
                        <span>
                          {milestone.plannedDate
                            ?? "No planned date"}
                        </span>

                        <h2>
                          {milestone.name}
                        </h2>
                      </div>

                      <strong className="statusBadge">
                        {milestone.status}
                      </strong>
                    </header>

                    <p>
                      {milestone.description
                        || "No description"}
                    </p>

                    <div className="taskProgressTrack">
                      <div
                        style={{
                          width: `${milestone.progressPercentage}%`,
                        }}
                      />
                    </div>

                    <div className="milestoneFooter">
                      <span>
                        {milestone.progressPercentage}%
                      </span>

                      <span>
                        {milestone.clientVisible
                          ? "Client visible"
                          : "Internal"}
                      </span>

                      <div>
                        {milestoneActions(
                          milestone.status
                        ).map(
                          (action) => (
                            <button
                              key={action.path}
                              type="button"
                              className="smallButton"
                              onClick={() =>
                                transition(
                                  milestone.id,
                                  action.path
                                )
                              }
                            >
                              {action.label}
                            </button>
                          )
                        )}
                      </div>
                    </div>
                  </div>
                </article>
              )
            )}
          </div>

          <aside className="moduleFormCard">
            <h2>
              Create milestone
            </h2>

            <form
              className="authForm"
              onSubmit={createMilestone}
            >
              <label>
                Name
                <input
                  required
                  value={name}
                  onChange={(event) =>
                    setName(
                      event.target.value
                    )
                  }
                />
              </label>

              <label>
                Planned date
                <input
                  type="date"
                  value={plannedDate}
                  onChange={(event) =>
                    setPlannedDate(
                      event.target.value
                    )
                  }
                />
              </label>

              <label>
                Description
                <textarea
                  value={description}
                  onChange={(event) =>
                    setDescription(
                      event.target.value
                    )
                  }
                />
              </label>

              <label className="checkboxLabel">
                <input
                  type="checkbox"
                  checked={clientVisible}
                  onChange={(event) =>
                    setClientVisible(
                      event.target.checked
                    )
                  }
                />

                Visible to client
              </label>

              <button
                className="authSubmit"
                type="submit"
              >
                Create milestone
              </button>
            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}
