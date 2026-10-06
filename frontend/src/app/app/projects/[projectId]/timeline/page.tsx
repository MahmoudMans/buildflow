"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { ProjectTimelineView } from "@/components/ProjectTimeline";
import { ProjectWorkspaceNav } from "@/components/ProjectWorkspaceNav";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";

import { ApiClientError, apiGet } from "@/lib/api";
import { buildProjectTimeline, todayIso } from "@/lib/project-timeline";
import type { Milestone, ProjectView, TaskSummary } from "@/lib/types";

type Loaded = {
  project: ProjectView;
  tasks: TaskSummary[];
  milestones: Milestone[];
  today: string;
};

export default function ProjectTimelinePage() {
  const params = useParams();
  const projectId = String(params.projectId);

  const { user, organizations, loading } = useAuth();

  const organizationSelection = useOrganizationSelection(organizations);
  const organizationId = organizationSelection.organizationId;

  const [data, setData] = useState<Loaded | null>(null);
  const [error, setError] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const [showCompleted, setShowCompleted] = useState(true);
  const [showCancelled, setShowCancelled] = useState(false);
  const requestSequence = useRef(0);

  const load = useCallback(async () => {
    if (!organizationId || !projectId) {
      return;
    }

    const requestId = requestSequence.current + 1;

    requestSequence.current = requestId;

    setRefreshing(true);
    setError("");

    try {
      const [project, tasks, milestones] = await Promise.all([
        apiGet<ProjectView>(`/projects/${projectId}`, organizationId),
        apiGet<TaskSummary[]>(`/projects/${projectId}/tasks`, organizationId),
        apiGet<Milestone[]>(
          `/projects/${projectId}/milestones`,
          organizationId,
        ),
      ]);

      if (requestSequence.current !== requestId) {
        return;
      }

      setData({ project, tasks, milestones, today: todayIso() });
    } catch (caught) {
      if (requestSequence.current !== requestId) {
        return;
      }

      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load the timeline.",
      );
    } finally {
      if (requestSequence.current === requestId) {
        setRefreshing(false);
      }
    }
  }, [organizationId, projectId]);

  useEffect(() => {
    if (!organizationId || !projectId) {
      return;
    }

    const timer = window.setTimeout(() => {
      void load();
    }, 0);

    return () => {
      window.clearTimeout(timer);
    };
  }, [organizationId, projectId, load]);

  const model = useMemo(
    () =>
      data
        ? buildProjectTimeline({
            project: data.project,
            tasks: data.tasks,
            milestones: data.milestones,
            options: { today: data.today, showCompleted, showCancelled },
          })
        : null,
    [data, showCompleted, showCancelled],
  );

  if (loading || !user) {
    return <main className="appLoading">Loading...</main>;
  }

  return (
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>{data?.project.projectNumber ?? "Project"}</p>

            <h1>Timeline</h1>

            {data && (
              <span className="projectClientName">{data.project.name}</span>
            )}
          </div>
        </header>

        <ProjectWorkspaceNav projectId={projectId} active="timeline" />

        {error && (
          <div className="formError moduleError">
            {error}{" "}
            <button
              type="button"
              className="uxLinkButton"
              onClick={() => {
                void load();
              }}
            >
              Try again
            </button>
          </div>
        )}

        {!data && !error && (
          <p className="uxTimelineNote" role="status">
            Loading the schedule...
          </p>
        )}

        {data && model && (
          <>
            <div className="uxTimelineToolbar">
              <label>
                <input
                  type="checkbox"
                  checked={showCompleted}
                  onChange={(event) => setShowCompleted(event.target.checked)}
                />
                Show completed tasks
              </label>

              <label>
                <input
                  type="checkbox"
                  checked={showCancelled}
                  onChange={(event) => setShowCancelled(event.target.checked)}
                />
                Show cancelled
              </label>

              <button
                type="button"
                className="uxSecondaryButton"
                disabled={refreshing}
                onClick={() => {
                  void load();
                }}
              >
                {refreshing ? "Refreshing..." : "Refresh"}
              </button>

              <Link
                className="uxLinkButton"
                href={`/app/projects/${projectId}/tasks`}
              >
                Edit task dates
              </Link>
            </div>

            <ProjectTimelineView model={model} projectId={projectId} />
          </>
        )}
      </section>
    </main>
  );
}
