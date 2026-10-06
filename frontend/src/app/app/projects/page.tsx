"use client";

import styles from "./projects.module.css";

import Link from "next/link";

import {
  FormEvent,
  useEffect,
  useRef,
  useMemo,
  useState,
} from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";
import { EmptyState } from "@/components/ux/EmptyState";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import { formatDate } from "@/lib/format";
import { humanizeStatus } from "@/lib/ux-validation";

import type {
  ClientSummary,
  Member,
  ProjectPriority,
  ProjectStatus,
  ProjectSummary,
  ProjectType,
} from "@/lib/types";

type SortKey = "newest" | "deadline" | "progress" | "name";

const STATUS_OPTIONS: ProjectStatus[] = [
  "DRAFT",
  "PLANNING",
  "ACTIVE",
  "ON_HOLD",
  "COMPLETED",
  "CANCELLED",
  "ARCHIVED",
];

const PRIORITY_OPTIONS: ProjectPriority[] = [
  "LOW",
  "MEDIUM",
  "HIGH",
  "CRITICAL",
];

const CLOSED_STATUSES: ProjectStatus[] = [
  "COMPLETED",
  "CANCELLED",
  "ARCHIVED",
];

const PROJECT_TYPES: { value: ProjectType; label: string }[] = [
  { value: "NEW_CONSTRUCTION", label: "New construction" },
  { value: "RENOVATION", label: "Renovation" },
  { value: "INTERIOR", label: "Interior" },
  { value: "ELECTRICAL", label: "Electrical" },
  { value: "PLUMBING", label: "Plumbing" },
  { value: "ROOFING", label: "Roofing" },
  { value: "LANDSCAPING", label: "Landscaping" },
  { value: "MAINTENANCE", label: "Maintenance" },
  { value: "OTHER", label: "Other" },
];

const SORT_OPTIONS: { value: SortKey; label: string }[] = [
  { value: "newest", label: "Newest first" },
  { value: "deadline", label: "Nearest deadline" },
  { value: "progress", label: "Most progress" },
  { value: "name", label: "Name A to Z" },
];

function todayIso() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${now.getFullYear()}-${month}-${day}`;
}

function daysBetween(fromIso: string, toIso: string) {
  const from = new Date(`${fromIso}T00:00:00`).getTime();
  const to = new Date(`${toIso}T00:00:00`).getTime();

  return Math.round((to - from) / 86_400_000);
}

type DeadlineTone = "overdue" | "soon" | "normal" | "none";

function deadlineSignal(
  project: ProjectSummary,
  today: string,
): { label: string; tone: DeadlineTone } {
  if (!project.plannedEndDate) {
    return { label: "No deadline", tone: "none" };
  }

  const formatted = formatDate(project.plannedEndDate);

  if (!today || CLOSED_STATUSES.includes(project.status)) {
    return { label: formatted, tone: "normal" };
  }

  const days = daysBetween(today, project.plannedEndDate);

  if (days < 0) {
    return {
      label: `${formatted} · ${Math.abs(days)}d overdue`,
      tone: "overdue",
    };
  }

  if (days <= 14) {
    return {
      label: `${formatted} · ${days === 0 ? "due today" : `${days}d left`}`,
      tone: "soon",
    };
  }

  return { label: formatted, tone: "normal" };
}

export default function ProjectsPage() {
  const [creatingProject, setCreatingProject] = useState(false);

  const { user, organizations, loading } = useAuth();

  const organizationSelection = useOrganizationSelection(organizations);

  const organizationId = organizationSelection.organizationId;

  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [clients, setClients] = useState<ClientSummary[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [today, setToday] = useState("");

  const [clientId, setClientId] = useState("");
  const [name, setName] = useState("");
  const [projectType, setProjectType] = useState<ProjectType>("RENOVATION");
  const [priority, setPriority] = useState<ProjectPriority>("MEDIUM");
  const [projectManagerMembershipId, setProjectManagerMembershipId] =
    useState("");
  const [startDate, setStartDate] = useState("");
  const [plannedEndDate, setPlannedEndDate] = useState("");
  const [error, setError] = useState("");

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | ProjectStatus>(
    "ALL",
  );
  const [priorityFilter, setPriorityFilter] = useState<
    "ALL" | ProjectPriority
  >("ALL");
  const [sort, setSort] = useState<SortKey>("newest");

  const filtersActive =
    search.trim() !== "" ||
    statusFilter !== "ALL" ||
    priorityFilter !== "ALL";

  function resetFilters() {
    setSearch("");
    setStatusFilter("ALL");
    setPriorityFilter("ALL");
  }

  const visibleProjects = useMemo(() => {
    const query = search.trim().toLowerCase();

    const filtered = projects.filter((project) => {
      if (statusFilter !== "ALL" && project.status !== statusFilter) {
        return false;
      }

      if (priorityFilter !== "ALL" && project.priority !== priorityFilter) {
        return false;
      }

      if (!query) {
        return true;
      }

      return (
        project.name.toLowerCase().includes(query) ||
        project.projectNumber.toLowerCase().includes(query) ||
        project.clientName.toLowerCase().includes(query)
      );
    });

    const sorted = [...filtered];

    if (sort === "deadline") {
      sorted.sort((a, b) =>
        (a.plannedEndDate ?? "9999-12-31").localeCompare(
          b.plannedEndDate ?? "9999-12-31",
        ),
      );
    } else if (sort === "progress") {
      sorted.sort((a, b) => b.progressPercentage - a.progressPercentage);
    } else if (sort === "name") {
      sorted.sort((a, b) => a.name.localeCompare(b.name));
    } else {
      sorted.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    }

    return sorted;
  }, [projects, search, statusFilter, priorityFilter, sort]);

  async function loadData() {
    try {
      const [projectResult, clientResult, memberResult] = await Promise.all([
        apiGet<ProjectSummary[]>("/projects", organizationId),

        apiGet<ClientSummary[]>("/clients", organizationId),

        apiGet<Member[]>(`/organizations/${organizationId}/members`),
      ]);

      const activeClients = clientResult.filter(
        (client) => client.status === "ACTIVE",
      );

      setProjects(projectResult);
      setToday(todayIso());
      setClients(activeClients);

      setMembers(
        memberResult.filter(
          (member) =>
            member.status === "ACTIVE" &&
            (member.role === "OWNER" ||
              member.role === "ADMIN" ||
              member.role === "PROJECT_MANAGER"),
        ),
      );

      if (!clientId && activeClients.length > 0) {
        setClientId(activeClients[0].id);
      }
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load projects.",
      );
    }
  }

  async function createProject(event: FormEvent) {
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
          startDate: startDate || null,
          plannedEndDate: plannedEndDate || null,
          projectManagerMembershipId: projectManagerMembershipId || null,
        },
        organizationId,
      );

      setName("");
      setStartDate("");
      setPlannedEndDate("");
      setProjectManagerMembershipId("");
      resetFilters();

      await loadData();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Project creation failed.",
      );
    }
  }

  // The effect below only re-runs when the organization changes, but it must
  // always call the latest loadData, so that is kept in a ref.
  const loadDataRef = useRef(loadData);

  useEffect(() => {
    loadDataRef.current = loadData;
  });

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (!organizationId) {
        return;
      }

      void loadDataRef.current();
    }, 0);

    return () => {
      window.clearTimeout(timer);
    };
  }, [organizationId]);

  if (loading || !user) {
    return <main className="appLoading">Loading...</main>;
  }

  const countLabel = filtersActive
    ? `${visibleProjects.length} of ${projects.length} projects`
    : projects.length === 1
      ? "1 project shown"
      : `${projects.length} projects shown`;

  return (
    <main className={`appShell ${styles.projectsShell}`}>
      <AppSidebar active="projects" />

      <section className={`appContent ${styles.projectsContent}`}>
        <header className="appHeader">
          <div
            id="new-project"
            data-buildflow-region="new-project"
            tabIndex={-1}
          >
            <h1>Construction projects</h1>

            <p className={styles.pageDescription}>
              Review projects for the selected organization.
            </p>

            <a className={styles.mobileNewProjectLink} href="#new-project">
              New project
            </a>
          </div>
        </header>

        {error && <div className="formError moduleError">{error}</div>}

        <section className="moduleLayout">
          <div className="moduleTableCard">
            <div className="uxToolbar" role="search" aria-label="Filter projects">
              <label>
                Search
                <input
                  type="search"
                  placeholder="Name, number or client"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </label>

              <label>
                Status
                <select
                  value={statusFilter}
                  onChange={(event) =>
                    setStatusFilter(
                      event.target.value as "ALL" | ProjectStatus,
                    )
                  }
                >
                  <option value="ALL">All statuses</option>

                  {STATUS_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {humanizeStatus(option)}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Priority
                <select
                  value={priorityFilter}
                  onChange={(event) =>
                    setPriorityFilter(
                      event.target.value as "ALL" | ProjectPriority,
                    )
                  }
                >
                  <option value="ALL">All priorities</option>

                  {PRIORITY_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {humanizeStatus(option)}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Sort by
                <select
                  value={sort}
                  onChange={(event) => setSort(event.target.value as SortKey)}
                >
                  {SORT_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div
              className="projectList"
              data-buildflow-region="project-list"
              aria-label="Project list"
            >
              <header className={styles.projectListCount}>
                <strong>{countLabel}</strong>

                {filtersActive && (
                  <button
                    type="button"
                    className="uxLinkButton"
                    onClick={resetFilters}
                  >
                    Clear filters
                  </button>
                )}
              </header>

              {projects.length === 0 && (
                <EmptyState
                  compact
                  title="No projects yet"
                  description="Use the New project form to create the first project for this organization."
                />
              )}

              {projects.length > 0 && visibleProjects.length === 0 && (
                <EmptyState
                  compact
                  title="No projects match your filters"
                  description="Try a different search or clear the filters."
                  action={
                    <button
                      type="button"
                      className="uxLinkButton"
                      onClick={resetFilters}
                    >
                      Clear filters
                    </button>
                  }
                />
              )}

              {visibleProjects.map((project) => {
                const progress = Math.max(
                  0,
                  Math.min(100, Math.round(project.progressPercentage)),
                );

                const deadline = deadlineSignal(project, today);

                return (
                  <Link
                    className="projectListCard"
                    key={project.id}
                    href={`/app/projects/${project.id}`}
                  >
                    <div className="projectListTop">
                      <div>
                        <span data-buildflow-project-reference="true">
                          {project.projectNumber}
                        </span>

                        <h3 data-buildflow-project-title="true">
                          {project.name}
                        </h3>

                        <p>Client: {project.clientName}</p>
                      </div>

                      <strong
                        className="statusBadge"
                        data-buildflow-project-status="true"
                      >
                        {humanizeStatus(project.status)}
                      </strong>
                    </div>

                    <div className="uxProjectProgress">
                      <div
                        className="uxProgressTrack"
                        role="progressbar"
                        aria-label={`${project.name} progress`}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-valuenow={progress}
                      >
                        <div
                          className={
                            progress >= 100
                              ? "uxProgressFill complete"
                              : "uxProgressFill"
                          }
                          style={{ width: `${progress}%` }}
                        />
                      </div>
                    </div>

                    <div
                      className="projectListMeta"
                      data-buildflow-metadata-grid="true"
                    >
                      <span
                        data-buildflow-meta="type"
                        data-buildflow-label="Type"
                      >
                        {humanizeStatus(project.projectType)}
                      </span>

                      <span
                        className={`uxPriority ${project.priority.toLowerCase()}`}
                        data-buildflow-meta="priority"
                        data-buildflow-label="Priority"
                      >
                        {humanizeStatus(project.priority)}
                      </span>

                      <span
                        className={`uxDeadline ${deadline.tone}`}
                        data-buildflow-meta="planned-end"
                        data-buildflow-label="Planned end"
                      >
                        {deadline.label}
                      </span>

                      <span
                        data-buildflow-meta="progress"
                        data-buildflow-label="Progress"
                      >
                        {progress}%
                      </span>
                    </div>
                  </Link>
                );
              })}
            </div>
          </div>

          <aside className="moduleFormCard">
            <h2>New project</h2>

            <p className={styles.creationOrganization}>
              Organization:{" "}
              {organizations.find(
                (organization) => organization.id === organizationId,
              )?.name ?? "Current organization"}
            </p>

            <p className={styles.requiredInstruction}>
              Fields marked <strong aria-hidden="true">*</strong> are required.
            </p>

            <form
              className="authForm"
              onSubmit={async (event) => {
                if (creatingProject) {
                  event.preventDefault();
                  return;
                }

                setCreatingProject(true);

                try {
                  await createProject(event);
                } finally {
                  setCreatingProject(false);
                }
              }}
              data-buildflow-create-form="true"
            >
              <label data-buildflow-required="true">
                Client
                <select
                  required
                  value={clientId}
                  onChange={(event) => setClientId(event.target.value)}
                >
                  <option value="">Select client</option>

                  {clients.map((client) => (
                    <option key={client.id} value={client.id}>
                      {client.displayName}
                    </option>
                  ))}
                </select>
              </label>

              <label data-buildflow-required="true">
                Project name
                <input
                  required
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                />
              </label>

              <div className="fieldRow">
                <label>
                  Type
                  <select
                    value={projectType}
                    onChange={(event) =>
                      setProjectType(event.target.value as ProjectType)
                    }
                  >
                    {PROJECT_TYPES.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Priority
                  <select
                    value={priority}
                    onChange={(event) =>
                      setPriority(event.target.value as ProjectPriority)
                    }
                  >
                    {PRIORITY_OPTIONS.map((option) => (
                      <option key={option} value={option}>
                        {humanizeStatus(option)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <label>
                Project manager
                <select
                  value={projectManagerMembershipId}
                  onChange={(event) =>
                    setProjectManagerMembershipId(event.target.value)
                  }
                >
                  <option value="">Assign later</option>

                  {members.map((member) => (
                    <option key={member.membershipId} value={member.membershipId}>
                      {member.firstName} {member.lastName}
                    </option>
                  ))}
                </select>
              </label>

              <div className="fieldRow">
                <label data-buildflow-date-field="start">
                  Start date
                  <input
                    type="date"
                    value={startDate}
                    onChange={(event) => setStartDate(event.target.value)}
                  />
                </label>

                <label data-buildflow-date-field="planned-end">
                  Planned end
                  <input
                    type="date"
                    value={plannedEndDate}
                    onChange={(event) => setPlannedEndDate(event.target.value)}
                  />
                </label>
              </div>

              <button
                className="authSubmit"
                type="submit"
                disabled={!clientId || creatingProject}
                aria-busy={creatingProject}
              >
                {creatingProject ? "Creating project…" : "Create project"}
              </button>

              <span
                className={styles.creationStatus}
                role="status"
                aria-live="polite"
                aria-atomic="true"
              >
                {creatingProject ? "Creating project…" : ""}
              </span>
            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}
