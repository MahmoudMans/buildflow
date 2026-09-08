"use client";

import Link from "next/link";

import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import { useParams } from "next/navigation";

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
  Member,
  ProjectMember,
  ProjectRole,
  ProjectStatus,
  ProjectView,
} from "@/lib/types";

const projectRoles: ProjectRole[] = [
  "PROJECT_MANAGER",
  "SITE_MANAGER",
  "ENGINEER",
  "FOREMAN",
  "WORKER",
  "ACCOUNTANT",
  "VIEWER",
];

function transitionAction(
  status: ProjectStatus
): {
  label: string;
  path: string;
}[] {
  switch (status) {
    case "DRAFT":
      return [
        {
          label: "Move to planning",
          path: "plan",
        },
        {
          label: "Cancel project",
          path: "cancel",
        },
      ];

    case "PLANNING":
      return [
        {
          label: "Activate project",
          path: "activate",
        },
        {
          label: "Put on hold",
          path: "hold",
        },
        {
          label: "Cancel project",
          path: "cancel",
        },
      ];

    case "ACTIVE":
      return [
        {
          label: "Put on hold",
          path: "hold",
        },
        {
          label: "Complete project",
          path: "complete",
        },
        {
          label: "Cancel project",
          path: "cancel",
        },
      ];

    case "ON_HOLD":
      return [
        {
          label: "Resume project",
          path: "resume",
        },
        {
          label: "Cancel project",
          path: "cancel",
        },
      ];

    case "COMPLETED":
      return [
        {
          label: "Archive project",
          path: "archive",
        },
      ];

    default:
      return [];
  }
}

export default function ProjectDetailPage() {
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

  const [
    projectMembers,
    setProjectMembers,
  ] = useState<ProjectMember[]>([]);

  const [
    organizationMembers,
    setOrganizationMembers,
  ] = useState<Member[]>([]);

  const [membershipId, setMembershipId] =
    useState("");

  const [projectRole, setProjectRole] =
    useState<ProjectRole>(
      "WORKER"
    );

  const [error, setError] =
    useState("");



  async function loadProject() {
    try {
      const [
        projectResult,
        projectMemberResult,
        organizationMemberResult,
      ] = await Promise.all([
        apiGet<ProjectView>(
          `/projects/${projectId}`,
          organizationId
        ),

        apiGet<ProjectMember[]>(
          `/projects/${projectId}/members`,
          organizationId
        ),

        apiGet<Member[]>(
          `/organizations/${organizationId}/members`
        ),
      ]);

      setProject(projectResult);

      setProjectMembers(
        projectMemberResult
      );

      setOrganizationMembers(
        organizationMemberResult.filter(
          (member) =>
            member.status === "ACTIVE"
            && member.role !== "CLIENT"
        )
      );
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load project."
      );
    }
  }

  async function changeStatus(
    path: string
  ) {
    try {
      await apiPost(
        `/projects/${projectId}/${path}`,
        undefined,
        organizationId
      );

      await loadProject();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Project status update failed."
      );
    }
  }

  async function addMember(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!membershipId) {
      return;
    }

    try {
      await apiPost(
        `/projects/${projectId}/members`,
        {
          membershipId,
          projectRole,
        },
        organizationId
      );

      setMembershipId("");

      await loadProject();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not assign project member."
      );
    }
  }

    /* eslint-disable react-hooks/exhaustive-deps */
useEffect(() => {
    // BUILDFLOW_ASYNC_LOAD_EFFECT
    const timer =
      window.setTimeout(
        () => {
        if (
          organizationId &&
          projectId
        ) {
          loadProject();
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
    loading ||
    !user ||
    !project
  ) {
    return (
      <main className="appLoading">
        {error || "Loading..."}
      </main>
    );
  }

  const assignedMembershipIds =
    new Set(
      projectMembers.map(
        (member) =>
          member.membershipId
      )
    );

  const availableMembers =
    organizationMembers.filter(
      (member) =>
        !assignedMembershipIds.has(
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
              {project.name}
            </h1>

            <span className="projectClientName">
              {project.clientName}
            </span>
          </div>

          <div className="projectActions">
            {transitionAction(
              project.status
            ).map((action) => (
              <button
                key={action.path}
                className="smallButton"
                type="button"
                onClick={() =>
                  changeStatus(
                    action.path
                  )
                }
              >
                {action.label}
              </button>
            ))}

            <Link
              className="smallButton"
              href="/app/projects"
            >
              Back
            </Link>
          </div>
        </header>

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <ProjectWorkspaceNav
          projectId={projectId}
          active="overview"
        />

        <section className="detailCards projectMetrics">
          <article>
            <span>Status</span>
            <strong>
              {project.status}
            </strong>
          </article>

          <article>
            <span>Progress</span>
            <strong>
              {project.progressPercentage}%
            </strong>
          </article>

          <article>
            <span>Priority</span>
            <strong>
              {project.priority}
            </strong>
          </article>

          <article>
            <span>Type</span>
            <strong>
              {project.projectType}
            </strong>
          </article>

          <article>
            <span>Start</span>
            <strong>
              {project.startDate ?? "—"}
            </strong>
          </article>

          <article>
            <span>Planned end</span>
            <strong>
              {project.plannedEndDate ??
                "—"}
            </strong>
          </article>
        </section>

        <section className="projectInfoCard">
          <div>
            <span>
              Location
            </span>

            <strong>
              {[
                project.siteAddress,
                project.city,
                project.country,
              ]
                .filter(Boolean)
                .join(", ")
                || "Not specified"}
            </strong>
          </div>

          <div>
            <span>
              Currency
            </span>

            <strong>
              {project.currency}
            </strong>
          </div>

          <div>
            <span>
              Description
            </span>

            <strong>
              {project.description ||
                "No description"}
            </strong>
          </div>
        </section>

        <section className="moduleLayout">
          <div className="moduleTableCard">
            <div className="moduleCardHeader">
              <div>
                <h2>
                  Project team
                </h2>

                <p>
                  {projectMembers.length} assigned members
                </p>
              </div>
            </div>

            <div className="dataList">
              {projectMembers.length ===
                0 && (
                <div className="emptyState">
                  No project members.
                </div>
              )}

              {projectMembers.map(
                (member) => (
                  <div
                    className="dataRow"
                    key={member.id}
                  >
                    <div>
                      <strong>
                        {member.firstName}
                        {" "}
                        {member.lastName}
                      </strong>

                      <span>
                        {member.email}
                      </span>
                    </div>

                    <span>
                      {member.projectRole}
                    </span>

                    <span>
                      Assigned
                    </span>

                    <span>
                      {project
                        .projectManagerMembershipId
                        === member.membershipId
                        ? "PRIMARY PM"
                        : ""}
                    </span>
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
              onSubmit={addMember}
            >
              <label>
                Organization member
                <select
                  required
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

              <label>
                Project role
                <select
                  value={projectRole}
                  onChange={(event) =>
                    setProjectRole(
                      event.target.value as ProjectRole
                    )
                  }
                >
                  {projectRoles.map(
                    (role) => (
                      <option
                        key={role}
                        value={role}
                      >
                        {role}
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
                Assign member
              </button>
            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}
