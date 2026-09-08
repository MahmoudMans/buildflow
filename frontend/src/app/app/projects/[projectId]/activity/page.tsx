"use client";

import {
  useCallback,
  useEffect,
  useState,
} from "react";

import {
  useParams,
} from "next/navigation";

import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  ProjectWorkspaceNav,
} from "@/components/ProjectWorkspaceNav";

import {
  useAuth,
} from "@/components/AuthProvider";

import {
  useOrganizationSelection,
} from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
} from "@/lib/api";

import type {
  ProjectActivity,
} from "@/lib/activity-types";

import type {
  ProjectView,
} from "@/lib/types";

export default function ProjectActivityPage() {
  const params = useParams();

  const projectId =
    String(params.projectId);

  const {
    user,
    organizations,
    loading,
  } = useAuth();

  const selection =
    useOrganizationSelection(
      organizations
    );

  const organizationId =
    selection.organizationId;

  const [project, setProject] =
    useState<ProjectView | null>(
      null
    );

  const [activity, setActivity] =
    useState<ProjectActivity[]>(
      []
    );

  const [error, setError] =
    useState("");

  const load =
    useCallback(
      async () => {
        if (!organizationId) {
          return;
        }

        try {
          setError("");

          const [
            projectResult,
            activityResult,
          ] =
            await Promise.all([
              apiGet<ProjectView>(
                `/projects/${projectId}`,
                organizationId
              ),

              apiGet<ProjectActivity[]>(
                `/projects/${projectId}/activity`,
                organizationId
              ),
            ]);

          setProject(projectResult);
          setActivity(
            activityResult
          );
        } catch (caught) {
          setError(
            caught
              instanceof ApiClientError
              ? caught.message
              : caught
                instanceof Error
                ? caught.message
                : "Unable to load activity."
          );
        }
      },
      [
        organizationId,
        projectId,
      ]
    );

  useEffect(() => {
    if (!organizationId) {
      return;
    }

    const timer =
      window.setTimeout(
        () => {
          void load();
        },
        0
      );

    return () => {
      window.clearTimeout(timer);
    };
  }, [
    organizationId,
    load,
  ]);

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
              Activity
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="activity"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="activityTimeline">
          {activity.length === 0 && (
            <div className="emptyState">
              No project activity yet.
            </div>
          )}

          {activity.map(
            (item) => (
              <article
                key={item.id}
                className="activityTimelineItem"
              >
                <div className="activityDot" />

                <div>
                  <header>
                    <strong>
                      {item.message}
                    </strong>

                    <span>
                      {
                        new Date(
                          item.createdAt
                        ).toLocaleString()
                      }
                    </span>
                  </header>

                  <small>
                    {item.action}
                  </small>
                </div>
              </article>
            )
          )}
        </section>
      </section>
    </main>
  );
}
