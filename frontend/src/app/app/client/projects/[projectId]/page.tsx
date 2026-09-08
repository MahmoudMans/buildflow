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
  apiPost,
} from "@/lib/api";

import type {
  ClientPortalProjectView,
} from "@/lib/client-portal-types";

import type {
  ProjectDocument,
  ProjectPhoto,
} from "@/lib/document-types";

export default function ClientProjectPage() {
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
    useState<
      ClientPortalProjectView | null
    >(null);

  const [documents, setDocuments] =
    useState<ProjectDocument[]>(
      []
    );

  const [photos, setPhotos] =
    useState<ProjectPhoto[]>(
      []
    );

  const [error, setError] =
    useState("");

  const handleError =
    useCallback(
      (
        caught: unknown
      ) => {
        setError(
          caught
            instanceof ApiClientError
            ? caught.message
            : caught
              instanceof Error
              ? caught.message
              : "Client portal operation failed."
        );
      },
      []
    );

  const loadData =
    useCallback(
      async () => {
        if (!organizationId) {
          return;
        }

        try {
          setError("");

          const [
            projectResult,
            documentResult,
            photoResult,
          ] =
            await Promise.all([
              apiGet<
                ClientPortalProjectView
              >(
                `/client-portal/projects/${projectId}`,
                organizationId
              ),

              apiGet<
                ProjectDocument[]
              >(
                `/client-portal/projects/${projectId}/documents`,
                organizationId
              ),

              apiGet<ProjectPhoto[]>(
                `/client-portal/projects/${projectId}/photos`,
                organizationId
              ),
            ]);

          setProject(projectResult);
          setDocuments(
            documentResult
          );
          setPhotos(photoResult);
        } catch (caught) {
          handleError(caught);
        }
      },
      [
        organizationId,
        projectId,
        handleError,
      ]
    );

  useEffect(() => {
    if (!organizationId) {
      return;
    }

    const timer =
      window.setTimeout(
        () => {
          void loadData();
        },
        0
      );

    return () => {
      window.clearTimeout(timer);
    };
  }, [
    organizationId,
    loadData,
  ]);

  async function decide(
    changeOrderId: string,
    action:
      | "approve"
      | "reject"
  ) {
    const comment =
      window.prompt(
        action === "approve"
          ? "Approval comment (optional):"
          : "Rejection comment (optional):"
      );

    if (comment === null) {
      return;
    }

    try {
      await apiPost(
        `/client-portal/projects/${projectId}/change-orders/${changeOrderId}/${action}`,
        {
          comment:
            comment.trim()
            || null,
        },
        organizationId
      );

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

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
    <main className="clientPortalShell">
      <header className="clientPortalHeader">
        <div>
          <span>
            {
              project
                .project
                .projectNumber
            }
          </span>

          <h1>
            {
              project
                .project
                .name
            }
          </h1>
        </div>

        <strong>
          {
            project
              .project
              .status
          }
        </strong>
      </header>

      <ClientPortalNav
        projectId={projectId}
      />

      {error && (
        <div className="formError">
          {error}
        </div>
      )}

      <section
        className="clientPortalOverview"
        id="overview"
      >
        <article>
          <span>
            Completion
          </span>

          <strong>
            {
              project
                .project
                .progressPercentage
              ?? 0
            }
            %
          </strong>
        </article>

        <article>
          <span>
            Expected completion
          </span>

          <strong>
            {
              project
                .project
                .plannedEndDate
              ?? "Not scheduled"
            }
          </strong>
        </article>

        <article>
          <span>
            Client documents
          </span>

          <strong>
            {documents.length}
          </strong>
        </article>

        <article>
          <span>
            Shared photos
          </span>

          <strong>
            {photos.length}
          </strong>
        </article>
      </section>

      <section
        className="clientPortalSection"
        id="progress"
      >
        <h2>
          Recent progress
        </h2>

        {project.recentUpdates.length
          === 0
          && (
            <div className="emptyState">
              No progress updates.
            </div>
          )}

        {project.recentUpdates.map(
          (update) => (
            <article
              key={update.id}
              className="clientPortalRow"
            >
              <strong>
                {
                  update
                    .progressPercentage
                }
                %
              </strong>

              <div>
                <p>
                  {update.summary}
                </p>

                {update.nextSteps && (
                  <small>
                    {
                      update
                        .nextSteps
                    }
                  </small>
                )}
              </div>
            </article>
          )
        )}
      </section>

      <section
        className="clientPortalSection"
        id="milestones"
      >
        <h2>
          Upcoming milestones
        </h2>

        {project.upcomingMilestones.length
          === 0
          && (
            <div className="emptyState">
              No shared upcoming milestones.
            </div>
          )}

        {project.upcomingMilestones.map(
          (milestone) => (
            <article
              key={milestone.id}
              className="clientPortalRow"
            >
              <strong>
                {milestone.status}
              </strong>

              <div>
                <p>
                  {milestone.name}
                </p>

                <small>
                  {
                    milestone
                      .plannedDate
                    ?? "No planned date"
                  }
                </small>
              </div>
            </article>
          )
        )}
      </section>

      <section
        className="clientPortalSection"
        id="photos"
      >
        <h2>
          Progress photos
        </h2>

        <div className="clientPortalPhotoGrid">
          {photos.map(
            (photo) => (
              <article
                key={photo.id}
                className="phase9PhotoCard"
              >
                <strong>
                  {photo.category}
                </strong>

                <p>
                  {
                    photo.caption
                    ?? "Project photo"
                  }
                </p>

                <span>
                  {
                    photo.takenDate
                    ?? "Date not specified"
                  }
                </span>
              </article>
            )
          )}
        </div>
      </section>

      <section
        className="clientPortalSection"
        id="documents"
      >
        <h2>
          Documents
        </h2>

        {documents.map(
          (document) => (
            <article
              key={document.id}
              className="clientPortalRow"
            >
              <strong>
                {document.category}
              </strong>

              <div>
                <p>
                  {document.name}
                </p>

                <small>
                  Version {
                    document.version
                  }
                </small>
              </div>
            </article>
          )
        )}
      </section>

      <section
        className="clientPortalSection"
        id="changes"
      >
        <h2>
          Change orders
        </h2>

        {project.changeOrders.map(
          (changeOrder) => (
            <article
              key={changeOrder.id}
              className="clientChangeOrderCard"
            >
              <header>
                <span>
                  {
                    changeOrder
                      .changeOrderNumber
                  }
                </span>

                <strong>
                  {
                    changeOrder
                      .status
                  }
                </strong>
              </header>

              <h3>
                {
                  changeOrder
                    .title
                }
              </h3>

              <p>
                {
                  changeOrder
                    .description
                }
              </p>

              <div>
                <span>
                  Client price impact
                </span>

                <strong>
                  {
                    changeOrder
                      .clientPriceImpact
                  }
                </strong>
              </div>

              {changeOrder.status
                === "CLIENT_REVIEW"
                && (
                  <footer>
                    <button
                      type="button"
                      onClick={() =>
                        void decide(
                          changeOrder.id,
                          "approve"
                        )
                      }
                    >
                      Approve
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        void decide(
                          changeOrder.id,
                          "reject"
                        )
                      }
                    >
                      Reject
                    </button>
                  </footer>
                )}
            </article>
          )
        )}
      </section>

      <section
        className="clientPortalSection"
        id="quotes"
      >
        <h2>
          Quotes
        </h2>

        {project.quotes.map(
          (quote) => (
            <article
              key={quote.id}
              className="clientPortalRow"
            >
              <strong>
                {
                  quote
                    .quoteNumber
                }
              </strong>

              <div>
                <p>
                  {quote.status}
                </p>

                <small>
                  Total: {
                    quote.total
                  }
                </small>
              </div>
            </article>
          )
        )}
      </section>

      <section
        className="clientPortalSection"
        id="invoices"
      >
        <h2>
          Invoices
        </h2>

        {project.invoices.map(
          (invoice) => (
            <article
              key={invoice.id}
              className="clientPortalRow"
            >
              <strong>
                {
                  invoice
                    .invoiceNumber
                }
              </strong>

              <div>
                <p>
                  {invoice.status}
                </p>

                <small>
                  Total: {
                    invoice.total
                  }
                  {" · Paid: "}
                  {
                    invoice
                      .amountPaid
                  }
                </small>
              </div>
            </article>
          )
        )}
      </section>
    </main>
  );
}
