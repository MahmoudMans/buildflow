"use client";

import {
  useCallback,
  useEffect,
  useState,
} from "react";

import Link from "next/link";

import {
  ClientPortalNav,
} from "@/components/ClientPortalNav";

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
  ClientPortalProjectSummary,
} from "@/lib/client-portal-types";

export default function ClientPortalPage() {
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

  const [projects, setProjects] =
    useState<
      ClientPortalProjectSummary[]
    >([]);

  const [error, setError] =
    useState("");

  const loadProjects =
    useCallback(
      async () => {
        if (!organizationId) {
          return;
        }

        try {
          setError("");

          const result =
            await apiGet<
              ClientPortalProjectSummary[]
            >(
              "/client-portal/projects",
              organizationId
            );

          setProjects(result);
        } catch (caught) {
          setError(
            caught
              instanceof ApiClientError
              ? caught.message
              : caught
                instanceof Error
                ? caught.message
                : "Unable to load client projects."
          );
        }
      },
      [organizationId]
    );

  useEffect(() => {
    if (!organizationId) {
      return;
    }

    const timer =
      window.setTimeout(
        () => {
          void loadProjects();
        },
        0
      );

    return () => {
      window.clearTimeout(timer);
    };
  }, [
    organizationId,
    loadProjects,
  ]);

  if (
    loading
    || !user
  ) {
    return (
      <main className="appLoading">
        Loading...
      </main>
    );
  }

  return (
    <main className="clientPortalShell">
      <header className="clientPortalHeader">
        <div>
          <span>
            BUILDFLOW CLIENT PORTAL
          </span>

          <h1>
            My projects
          </h1>
        </div>
      </header>

      <ClientPortalNav />

      {error && (
        <div className="formError">
          {error}
        </div>
      )}

      <section className="clientPortalProjects">
        {projects.length === 0 && !error && (
          <div className="emptyState">
            No projects have been authorized for this account.
          </div>
        )}

        {projects.map(
          (project) => (
            <Link
              key={project.id}
              className="clientProjectCard"
              href={`/app/client/projects/${project.id}`}
            >
              <header>
                <span>
                  {
                    project
                      .projectNumber
                  }
                </span>

                <strong>
                  {project.status}
                </strong>
              </header>

              <h2>
                {project.name}
              </h2>

              <div className="clientProgressTrack">
                <span
                  style={{
                    width:
                      `${project.progressPercentage ?? 0}%`,
                  }}
                />
              </div>

              <footer>
                <span>
                  {
                    project
                      .progressPercentage
                    ?? 0
                  }
                  % complete
                </span>

                <span>
                  {
                    project
                      .plannedEndDate
                    ?? "No expected completion"
                  }
                </span>
              </footer>
            </Link>
          )
        )}
      </section>
    </main>
  );
}
