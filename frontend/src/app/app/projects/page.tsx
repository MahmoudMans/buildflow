"use client";

import Link from "next/link";

import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  ClientSummary,
  Member,
  ProjectPriority,
  ProjectSummary,
  ProjectType,
} from "@/lib/types";

export default function ProjectsPage() {
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

  const [projects, setProjects] =
    useState<ProjectSummary[]>([]);

  const [clients, setClients] =
    useState<ClientSummary[]>([]);

  const [members, setMembers] =
    useState<Member[]>([]);

  const [clientId, setClientId] =
    useState("");

  const [name, setName] =
    useState("");

  const [projectType, setProjectType] =
    useState<ProjectType>(
      "RENOVATION"
    );

  const [priority, setPriority] =
    useState<ProjectPriority>(
      "MEDIUM"
    );

  const [
    projectManagerMembershipId,
    setProjectManagerMembershipId,
  ] = useState("");

  const [startDate, setStartDate] =
    useState("");

  const [
    plannedEndDate,
    setPlannedEndDate,
  ] = useState("");

  const [error, setError] =
    useState("");



  async function loadData() {
    try {
      const [
        projectResult,
        clientResult,
        memberResult,
      ] = await Promise.all([
        apiGet<ProjectSummary[]>(
          "/projects",
          organizationId
        ),

        apiGet<ClientSummary[]>(
          "/clients",
          organizationId
        ),

        apiGet<Member[]>(
          `/organizations/${organizationId}/members`
        ),
      ]);

      setProjects(
        projectResult
      );

      setClients(
        clientResult.filter(
          (client) =>
            client.status === "ACTIVE"
        )
      );

      setMembers(
        memberResult.filter(
          (member) =>
            member.status === "ACTIVE"
            && (
              member.role === "OWNER"
              || member.role === "ADMIN"
              || member.role ===
                "PROJECT_MANAGER"
            )
        )
      );

      if (
        !clientId
        && clientResult.length > 0
      ) {
        setClientId(
          clientResult[0].id
        );
      }
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load projects."
      );
    }
  }

  async function createProject(
    event: FormEvent
  ) {
    event.preventDefault();

    setError("");

    try {
      await apiPost(
        "/projects",
        {
          clientId,
          name,
          projectType,
          priority,
          startDate:
            startDate || null,
          plannedEndDate:
            plannedEndDate || null,
          projectManagerMembershipId:
            projectManagerMembershipId
              || null,
        },
        organizationId
      );

      setName("");
      setStartDate("");
      setPlannedEndDate("");
      setProjectManagerMembershipId(
        ""
      );

      await loadData();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Project creation failed."
      );
    }
  }

    /* eslint-disable react-hooks/exhaustive-deps */
useEffect(() => {
    // BUILDFLOW_ASYNC_LOAD_EFFECT
    const timer =
      window.setTimeout(
        () => {
        if (!organizationId) {
          return;
        }

        loadData();
        },
        0
      );

    return () => {
      window.clearTimeout(
        timer
      );
    };
  }, [organizationId]);
  /* eslint-enable react-hooks/exhaustive-deps */

  if (
    loading ||
    !user
  ) {
    return (
      <main className="appLoading">
        Loading...
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>PROJECTS</p>
            <h1>
              Construction projects
            </h1>
          </div>

          <select
            className="organizationSelector"
            value={organizationId}
            onChange={(event) => {
              const value =
                event.target.value;

              organizationSelection.selectOrganization(
                value
              );

              localStorage.setItem(
                "buildflow.organizationId",
                value
              );
            }}
          >
            {organizations.map(
              (organization) => (
                <option
                  key={organization.id}
                  value={organization.id}
                >
                  {organization.name}
                </option>
              )
            )}
          </select>
        </header>

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="moduleLayout">
          <div className="moduleTableCard">
            <div className="moduleCardHeader">
              <div>
                <h2>
                  Projects
                </h2>

                <p>
                  {projects.length} projects
                </p>
              </div>
            </div>

            <div className="projectList">
              {projects.length === 0 && (
                <div className="emptyState">
                  No projects yet.
                </div>
              )}

              {projects.map(
                (project) => (
                  <Link
                    className="projectListCard"
                    key={project.id}
                    href={`/app/projects/${project.id}`}
                  >
                    <div className="projectListTop">
                      <div>
                        <span>
                          {project.projectNumber}
                        </span>

                        <h3>
                          {project.name}
                        </h3>

                        <p>
                          {project.clientName}
                        </p>
                      </div>

                      <strong className="statusBadge">
                        {project.status}
                      </strong>
                    </div>

                    <div className="projectListMeta">
                      <span>
                        {project.projectType}
                      </span>

                      <span>
                        {project.priority}
                      </span>

                      <span>
                        {project.plannedEndDate ??
                          "No deadline"}
                      </span>

                      <span>
                        {project.progressPercentage}%
                      </span>
                    </div>
                  </Link>
                )
              )}
            </div>
          </div>

          <aside className="moduleFormCard">
            <h2>New project</h2>

            <form
              className="authForm"
              onSubmit={createProject}
            >
              <label>
                Client
                <select
                  required
                  value={clientId}
                  onChange={(event) =>
                    setClientId(
                      event.target.value
                    )
                  }
                >
                  <option value="">
                    Select client
                  </option>

                  {clients.map(
                    (client) => (
                      <option
                        key={client.id}
                        value={client.id}
                      >
                        {client.displayName}
                      </option>
                    )
                  )}
                </select>
              </label>

              <label>
                Project name
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

              <div className="fieldRow">
                <label>
                  Type
                  <select
                    value={projectType}
                    onChange={(event) =>
                      setProjectType(
                        event.target.value as ProjectType
                      )
                    }
                  >
                    <option value="NEW_CONSTRUCTION">
                      New construction
                    </option>

                    <option value="RENOVATION">
                      Renovation
                    </option>

                    <option value="INTERIOR">
                      Interior
                    </option>

                    <option value="ELECTRICAL">
                      Electrical
                    </option>

                    <option value="PLUMBING">
                      Plumbing
                    </option>

                    <option value="ROOFING">
                      Roofing
                    </option>

                    <option value="LANDSCAPING">
                      Landscaping
                    </option>

                    <option value="MAINTENANCE">
                      Maintenance
                    </option>

                    <option value="OTHER">
                      Other
                    </option>
                  </select>
                </label>

                <label>
                  Priority
                  <select
                    value={priority}
                    onChange={(event) =>
                      setPriority(
                        event.target.value as ProjectPriority
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
              </div>

              <label>
                Project manager
                <select
                  value={
                    projectManagerMembershipId
                  }
                  onChange={(event) =>
                    setProjectManagerMembershipId(
                      event.target.value
                    )
                  }
                >
                  <option value="">
                    Assign later
                  </option>

                  {members.map(
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

              <div className="fieldRow">
                <label>
                  Start date
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
                  Planned end
                  <input
                    type="date"
                    value={plannedEndDate}
                    onChange={(event) =>
                      setPlannedEndDate(
                        event.target.value
                      )
                    }
                  />
                </label>
              </div>

              <button
                className="authSubmit"
                type="submit"
                disabled={!clientId}
              >
                Create project
              </button>
            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}
