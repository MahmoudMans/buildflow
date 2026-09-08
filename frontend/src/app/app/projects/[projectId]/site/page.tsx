"use client";

import {
  type FormEvent,
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
  apiPost,
} from "@/lib/api";

import type {
  ProjectView,
} from "@/lib/types";

import type {
  DailySiteReport,
  IncidentSeverity,
  IncidentType,
  ProjectProgressUpdate,
  SiteIncident,
  SitePhoto,
  SitePhotoCategory,
} from "@/lib/site-operation-types";

async function fileToBase64(
  file: File
): Promise<string> {
  return new Promise(
    (
      resolve,
      reject
    ) => {
      const reader =
        new FileReader();

      reader.onload = () => {
        if (
          typeof reader.result
            !== "string"
        ) {
          reject(
            new Error(
              "Unable to read photo."
            )
          );

          return;
        }

        resolve(
          reader.result
        );
      };

      reader.onerror = () => {
        reject(
          reader.error
          ?? new Error(
            "Unable to read photo."
          )
        );
      };

      reader.readAsDataURL(
        file
      );
    }
  );
}

export default function SiteOperationsPage() {
  const params = useParams();

  const projectId =
    String(
      params.projectId
    );

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

  const organization =
    organizations.find(
      (entry) =>
        entry.id
          === organizationId
    );

  const role =
    organization?.role;

  const canManage =
    role === "OWNER"
    || role === "ADMIN"
    || role === "PROJECT_MANAGER"
    || role === "SITE_MANAGER";

  const [project, setProject] =
    useState<ProjectView | null>(
      null
    );

  const [reports, setReports] =
    useState<DailySiteReport[]>(
      []
    );

  const [
    selectedReportId,
    setSelectedReportId,
  ] =
    useState<string | null>(
      null
    );

  const [photos, setPhotos] =
    useState<SitePhoto[]>(
      []
    );

  const [
    incidents,
    setIncidents,
  ] =
    useState<SiteIncident[]>(
      []
    );

  const [
    progressUpdates,
    setProgressUpdates,
  ] =
    useState<
      ProjectProgressUpdate[]
    >([]);

  const [error, setError] =
    useState("");

  const [reportDate, setReportDate] =
    useState(
      new Date()
        .toISOString()
        .slice(0, 10)
    );

  const [weatherText, setWeatherText] =
    useState("");

  const [
    workersOnSite,
    setWorkersOnSite,
  ] =
    useState("0");

  const [
    reportSummary,
    setReportSummary,
  ] =
    useState("");

  const [
    workCompleted,
    setWorkCompleted,
  ] =
    useState("");

  const [delays, setDelays] =
    useState("");

  const [issues, setIssues] =
    useState("");

  const [
    reportNextSteps,
    setReportNextSteps,
  ] =
    useState("");

  const [photoFile, setPhotoFile] =
    useState<File | null>(
      null
    );

  const [
    photoCaption,
    setPhotoCaption,
  ] =
    useState("");

  const [
    photoCategory,
    setPhotoCategory,
  ] =
    useState<SitePhotoCategory>(
      "PROGRESS"
    );

  const [
    photoClientVisible,
    setPhotoClientVisible,
  ] =
    useState(false);

  const [
    incidentType,
    setIncidentType,
  ] =
    useState<IncidentType>(
      "SITE_ISSUE"
    );

  const [
    incidentSeverity,
    setIncidentSeverity,
  ] =
    useState<IncidentSeverity>(
      "MEDIUM"
    );

  const [
    incidentTitle,
    setIncidentTitle,
  ] =
    useState("");

  const [
    incidentDescription,
    setIncidentDescription,
  ] =
    useState("");

  const [
    incidentClientVisible,
    setIncidentClientVisible,
  ] =
    useState(false);

  const [
    progressSummary,
    setProgressSummary,
  ] =
    useState("");

  const [
    progressBlockers,
    setProgressBlockers,
  ] =
    useState("");

  const [
    progressNextSteps,
    setProgressNextSteps,
  ] =
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
              : "The site operation failed."
        );
      },
      []
    );

  const loadPhotos =
    useCallback(
      async (
        reportId: string
      ) => {
        if (!organizationId) {
          return;
        }

        try {
          const result =
            await apiGet<
              SitePhoto[]
            >(
              `/site-reports/${reportId}/photos`,
              organizationId
            );

          setPhotos(
            result
          );
        } catch (caught) {
          handleError(caught);
        }
      },
      [
        organizationId,
        handleError,
      ]
    );

  const loadData =
    useCallback(
      async (
        preferredReportId?: string
      ) => {
        if (
          !organizationId
          || !projectId
        ) {
          return;
        }

        try {
          setError("");

          const [
            projectResult,
            reportResult,
            incidentResult,
            progressResult,
          ] =
            await Promise.all([
              apiGet<ProjectView>(
                `/projects/${projectId}`,
                organizationId
              ),

              apiGet<
                DailySiteReport[]
              >(
                `/projects/${projectId}/site-reports`,
                organizationId
              ),

              apiGet<
                SiteIncident[]
              >(
                `/projects/${projectId}/incidents`,
                organizationId
              ),

              apiGet<
                ProjectProgressUpdate[]
              >(
                `/projects/${projectId}/progress-updates`,
                organizationId
              ),
            ]);

          setProject(
            projectResult
          );

          setReports(
            reportResult
          );

          setIncidents(
            incidentResult
          );

          setProgressUpdates(
            progressResult
          );

          if (
            reportResult.length
              === 0
          ) {
            setSelectedReportId(
              null
            );

            setPhotos([]);

            return;
          }

          const nextReportId =
            preferredReportId
            && reportResult.some(
              (report) =>
                report.id
                  === preferredReportId
            )
              ? preferredReportId
              : reportResult[0].id;

          setSelectedReportId(
            nextReportId
          );

          await loadPhotos(
            nextReportId
          );
        } catch (caught) {
          handleError(caught);
        }
      },
      [
        organizationId,
        projectId,
        loadPhotos,
        handleError,
      ]
    );

  useEffect(() => {
    if (
      !organizationId
      || !projectId
    ) {
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
      window.clearTimeout(
        timer
      );
    };
  }, [
    organizationId,
    projectId,
    loadData,
  ]);

  const selectedReport =
    reports.find(
      (report) =>
        report.id
          === selectedReportId
    )
    ?? null;

  async function createReport(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      const result =
        await apiPost<
          DailySiteReport
        >(
          `/projects/${projectId}/site-reports`,
          {
            reportDate,
            weatherText:
              weatherText || null,
            workersOnSite:
              Number(
                workersOnSite
              ),
            summary:
              reportSummary,
            workCompleted:
              workCompleted || null,
            delays:
              delays || null,
            issues:
              issues || null,
            nextSteps:
              reportNextSteps
              || null,
          },
          organizationId
        );

      setWeatherText("");
      setWorkersOnSite("0");
      setReportSummary("");
      setWorkCompleted("");
      setDelays("");
      setIssues("");
      setReportNextSteps("");

      await loadData(
        result.id
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function uploadPhoto(
    event: FormEvent
  ) {
    event.preventDefault();

    if (
      !photoFile
      || !selectedReportId
    ) {
      setError(
        "Choose a site report and photo first."
      );

      return;
    }

    try {
      const base64Data =
        await fileToBase64(
          photoFile
        );

      await apiPost<SitePhoto>(
        `/site-reports/${selectedReportId}/photos`,
        {
          fileName:
            photoFile.name,
          mimeType:
            photoFile.type,
          base64Data,
          caption:
            photoCaption || null,
          category:
            photoCategory,
          clientVisible:
            photoClientVisible,
        },
        organizationId
      );

      setPhotoFile(null);
      setPhotoCaption("");
      setPhotoCategory(
        "PROGRESS"
      );
      setPhotoClientVisible(
        false
      );

      await loadPhotos(
        selectedReportId
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function createIncident(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPost<SiteIncident>(
        `/projects/${projectId}/incidents`,
        {
          type:
            incidentType,
          severity:
            incidentSeverity,
          title:
            incidentTitle,
          description:
            incidentDescription,
          occurredAt:
            null,
          assignedTo:
            null,
          clientVisible:
            incidentClientVisible,
        },
        organizationId
      );

      setIncidentType(
        "SITE_ISSUE"
      );

      setIncidentSeverity(
        "MEDIUM"
      );

      setIncidentTitle("");
      setIncidentDescription("");
      setIncidentClientVisible(
        false
      );

      await loadData(
        selectedReportId
        ?? undefined
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function incidentAction(
    incidentId: string,
    action:
      | "investigate"
      | "action-required"
      | "close"
  ) {
    try {
      await apiPost<SiteIncident>(
        `/incidents/${incidentId}/${action}`,
        undefined,
        organizationId
      );

      await loadData(
        selectedReportId
        ?? undefined
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function resolveIncident(
    incidentId: string
  ) {
    const resolution =
      window.prompt(
        "Resolution:"
      );

    if (
      resolution === null
      || !resolution.trim()
    ) {
      return;
    }

    try {
      await apiPost<SiteIncident>(
        `/incidents/${incidentId}/resolve`,
        {
          resolution,
        },
        organizationId
      );

      await loadData(
        selectedReportId
        ?? undefined
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  async function createProgressUpdate(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPost<
        ProjectProgressUpdate
      >(
        `/projects/${projectId}/progress-updates`,
        {
          summary:
            progressSummary,
          blockers:
            progressBlockers
            || null,
          nextSteps:
            progressNextSteps
            || null,
        },
        organizationId
      );

      setProgressSummary("");
      setProgressBlockers("");
      setProgressNextSteps("");

      await loadData(
        selectedReportId
        ?? undefined
      );
    } catch (caught) {
      handleError(caught);
    }
  }

  function formatDate(
    value: string
  ) {
    return new Intl.DateTimeFormat(
      "en-GB",
      {
        dateStyle: "medium",
      }
    ).format(
      new Date(value)
    );
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
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent siteOpsContent">
        <header className="appHeader">
          <div>
            <p>
              {project.projectNumber}
            </p>

            <h1>
              Site operations
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="site"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="siteOpsQuickStats">
          <article>
            <span>
              Daily reports
            </span>

            <strong>
              {reports.length}
            </strong>
          </article>

          <article>
            <span>
              Open incidents
            </span>

            <strong>
              {
                incidents.filter(
                  (incident) =>
                    incident.status
                      !== "CLOSED"
                    && incident.status
                      !== "RESOLVED"
                ).length
              }
            </strong>
          </article>

          <article>
            <span>
              Critical incidents
            </span>

            <strong>
              {
                incidents.filter(
                  (incident) =>
                    incident.severity
                      === "CRITICAL"
                    && incident.status
                      !== "CLOSED"
                ).length
              }
            </strong>
          </article>

          <article>
            <span>
              Latest progress
            </span>

            <strong>
              {
                progressUpdates[0]
                  ?.progressPercentageSnapshot
                ?? project.progressPercentage
                ?? 0
              }
              %
            </strong>
          </article>
        </section>

        <section className="siteOpsMobileTabs">
          <a href="#daily-reports">
            Reports
          </a>

          <a href="#site-photos">
            Photos
          </a>

          <a href="#site-incidents">
            Incidents
          </a>

          <a href="#progress-updates">
            Progress
          </a>
        </section>

        <section
          className="siteOpsSection"
          id="daily-reports"
        >
          <div className="commercialSectionHeader">
            <div>
              <p>
                DAILY ACTIVITY
              </p>

              <h2>
                Site reports
              </h2>
            </div>

            <span>
              {reports.length}
            </span>
          </div>

          <div className="siteOpsGrid">
            <div className="siteReportList">
              {reports.length === 0 && (
                <div className="emptyState">
                  No daily reports yet.
                </div>
              )}

              {reports.map(
                (report) => (
                  <button
                    key={report.id}
                    type="button"
                    className={
                      selectedReportId
                        === report.id
                        ? "siteReportCard active"
                        : "siteReportCard"
                    }
                    onClick={() => {
                      setSelectedReportId(
                        report.id
                      );

                      void loadPhotos(
                        report.id
                      );
                    }}
                  >
                    <div>
                      <strong>
                        {
                          report
                            .reportDate
                        }
                      </strong>

                      <span>
                        {
                          report
                            .weatherText
                          ?? "Weather not recorded"
                        }
                      </span>
                    </div>

                    <span>
                      {
                        report
                          .workersOnSite
                      }
                      {" workers"}
                    </span>

                    <p>
                      {report.summary}
                    </p>
                  </button>
                )
              )}

              {selectedReport && (
                <article className="siteReportDetail">
                  <header>
                    <div>
                      <span>
                        DAILY REPORT
                      </span>

                      <h3>
                        {
                          selectedReport
                            .reportDate
                        }
                      </h3>
                    </div>

                    <strong>
                      {
                        selectedReport
                          .workersOnSite
                      }
                      {" on site"}
                    </strong>
                  </header>

                  <div className="siteReportDetailGrid">
                    <div>
                      <span>
                        Weather
                      </span>

                      <p>
                        {
                          selectedReport
                            .weatherText
                          ?? "Not recorded"
                        }
                      </p>
                    </div>

                    <div>
                      <span>
                        Work completed
                      </span>

                      <p>
                        {
                          selectedReport
                            .workCompleted
                          ?? "—"
                        }
                      </p>
                    </div>

                    <div>
                      <span>
                        Delays
                      </span>

                      <p>
                        {
                          selectedReport
                            .delays
                          ?? "None reported"
                        }
                      </p>
                    </div>

                    <div>
                      <span>
                        Issues
                      </span>

                      <p>
                        {
                          selectedReport
                            .issues
                          ?? "None reported"
                        }
                      </p>
                    </div>

                    <div>
                      <span>
                        Next steps
                      </span>

                      <p>
                        {
                          selectedReport
                            .nextSteps
                          ?? "—"
                        }
                      </p>
                    </div>
                  </div>
                </article>
              )}
            </div>

            {canManage && (
              <form
                className="commercialForm siteMobileForm"
                onSubmit={createReport}
              >
                <h3>
                  New daily report
                </h3>

                <label>
                  Date
                  <input
                    required
                    type="date"
                    value={reportDate}
                    onChange={(event) =>
                      setReportDate(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Weather
                  <input
                    maxLength={500}
                    value={weatherText}
                    placeholder="Dry, rain, wind..."
                    onChange={(event) =>
                      setWeatherText(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Workers on site
                  <input
                    required
                    type="number"
                    min="0"
                    value={workersOnSite}
                    onChange={(event) =>
                      setWorkersOnSite(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Summary
                  <textarea
                    required
                    value={reportSummary}
                    onChange={(event) =>
                      setReportSummary(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Work completed
                  <textarea
                    value={workCompleted}
                    onChange={(event) =>
                      setWorkCompleted(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Delays
                  <textarea
                    value={delays}
                    onChange={(event) =>
                      setDelays(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Issues
                  <textarea
                    value={issues}
                    onChange={(event) =>
                      setIssues(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Next steps
                  <textarea
                    value={reportNextSteps}
                    onChange={(event) =>
                      setReportNextSteps(
                        event.target.value
                      )
                    }
                  />
                </label>

                <button type="submit">
                  Save daily report
                </button>
              </form>
            )}
          </div>
        </section>

        <section
          className="siteOpsSection"
          id="site-photos"
        >
          <div className="commercialSectionHeader">
            <div>
              <p>
                PHOTO EVIDENCE
              </p>

              <h2>
                Daily report photos
              </h2>
            </div>

            <span>
              {photos.length}
            </span>
          </div>

          <div className="siteOpsGrid">
            <div className="sitePhotoList">
              {!selectedReportId && (
                <div className="emptyState">
                  Select a daily report to manage photos.
                </div>
              )}

              {selectedReportId
                && photos.length === 0
                && (
                  <div className="emptyState">
                    No photos attached to this report.
                  </div>
                )}

              {photos.map(
                (photo) => (
                  <article
                    key={photo.id}
                    className="sitePhotoCard"
                  >
                    <div>
                      <strong>
                        {photo.fileName}
                      </strong>

                      <span>
                        {photo.category}
                      </span>
                    </div>

                    <p>
                      {
                        photo.caption
                        ?? "No caption"
                      }
                    </p>

                    <small>
                      {
                        Math.ceil(
                          photo.sizeBytes
                            / 1024
                        )
                      }
                      {" KB"}
                    </small>
                  </article>
                )
              )}
            </div>

            {canManage
              && selectedReportId
              && (
                <form
                  className="commercialForm siteMobileForm"
                  onSubmit={uploadPhoto}
                >
                  <h3>
                    Add site photo
                  </h3>

                  <label>
                    Photo
                    <input
                      required
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={(event) =>
                        setPhotoFile(
                          event.target
                            .files?.[0]
                          ?? null
                        )
                      }
                    />
                  </label>

                  <label>
                    Category
                    <select
                      value={photoCategory}
                      onChange={(event) =>
                        setPhotoCategory(
                          event.target.value as SitePhotoCategory
                        )
                      }
                    >
                      <option value="BEFORE">
                        Before work
                      </option>

                      <option value="PROGRESS">
                        Progress
                      </option>

                      <option value="AFTER">
                        Completed
                      </option>

                      <option value="DELIVERY">
                        Delivery
                      </option>

                      <option value="DAMAGE">
                        Damage
                      </option>

                      <option value="SAFETY_ISSUE">
                        Safety issue
                      </option>

                      <option value="OTHER">
                        Other
                      </option>
                    </select>
                  </label>

                  <label>
                    Caption
                    <input
                      maxLength={1000}
                      value={photoCaption}
                      onChange={(event) =>
                        setPhotoCaption(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <label className="siteCheckbox">
                    <input
                      type="checkbox"
                      checked={
                        photoClientVisible
                      }
                      onChange={(event) =>
                        setPhotoClientVisible(
                          event.target
                            .checked
                        )
                      }
                    />
                    Client visible
                  </label>

                  <button type="submit">
                    Upload photo
                  </button>
                </form>
              )}
          </div>
        </section>

        <section
          className="siteOpsSection"
          id="site-incidents"
        >
          <div className="commercialSectionHeader">
            <div>
              <p>
                SITE CONTROL
              </p>

              <h2>
                Incidents
              </h2>
            </div>

            <span>
              {incidents.length}
            </span>
          </div>

          <div className="siteOpsGrid">
            <div className="siteIncidentList">
              {incidents.length === 0 && (
                <div className="emptyState">
                  No incidents reported.
                </div>
              )}

              {incidents.map(
                (incident) => (
                  <article
                    key={incident.id}
                    className={`siteIncidentCard severity-${incident.severity.toLowerCase()}`}
                  >
                    <header>
                      <div>
                        <span>
                          {
                            incident
                              .incidentNumber
                          }
                        </span>

                        <h3>
                          {
                            incident
                              .title
                          }
                        </h3>
                      </div>

                      <span className="statusBadge">
                        {
                          incident
                            .status
                        }
                      </span>
                    </header>

                    <div className="incidentMeta">
                      <span>
                        {
                          incident
                            .type
                        }
                      </span>

                      <strong>
                        {
                          incident
                            .severity
                        }
                      </strong>
                    </div>

                    <p>
                      {
                        incident
                          .description
                      }
                    </p>

                    {incident.resolution && (
                      <div className="incidentResolution">
                        <span>
                          Resolution
                        </span>

                        <p>
                          {
                            incident
                              .resolution
                          }
                        </p>
                      </div>
                    )}

                    {canManage && (
                      <div className="commercialActions siteIncidentActions">
                        {incident.status
                          === "OPEN"
                          && (
                            <>
                              <button
                                type="button"
                                onClick={() =>
                                  void incidentAction(
                                    incident.id,
                                    "investigate"
                                  )
                                }
                              >
                                Investigate
                              </button>

                              <button
                                type="button"
                                onClick={() =>
                                  void incidentAction(
                                    incident.id,
                                    "action-required"
                                  )
                                }
                              >
                                Require action
                              </button>

                              <button
                                type="button"
                                onClick={() =>
                                  void resolveIncident(
                                    incident.id
                                  )
                                }
                              >
                                Resolve
                              </button>
                            </>
                          )}

                        {incident.status
                          === "INVESTIGATING"
                          && (
                            <>
                              <button
                                type="button"
                                onClick={() =>
                                  void incidentAction(
                                    incident.id,
                                    "action-required"
                                  )
                                }
                              >
                                Require action
                              </button>

                              <button
                                type="button"
                                onClick={() =>
                                  void resolveIncident(
                                    incident.id
                                  )
                                }
                              >
                                Resolve
                              </button>
                            </>
                          )}

                        {incident.status
                          === "ACTION_REQUIRED"
                          && (
                            <>
                              <button
                                type="button"
                                onClick={() =>
                                  void incidentAction(
                                    incident.id,
                                    "investigate"
                                  )
                                }
                              >
                                Investigate
                              </button>

                              <button
                                type="button"
                                onClick={() =>
                                  void resolveIncident(
                                    incident.id
                                  )
                                }
                              >
                                Resolve
                              </button>
                            </>
                          )}

                        {incident.status
                          === "RESOLVED"
                          && (
                            <button
                              type="button"
                              onClick={() =>
                                void incidentAction(
                                  incident.id,
                                  "close"
                                )
                              }
                            >
                              Close
                            </button>
                          )}
                      </div>
                    )}
                  </article>
                )
              )}
            </div>

            {canManage && (
              <form
                className="commercialForm siteMobileForm"
                onSubmit={createIncident}
              >
                <h3>
                  Report incident
                </h3>

                <label>
                  Type
                  <select
                    value={incidentType}
                    onChange={(event) =>
                      setIncidentType(
                        event.target.value as IncidentType
                      )
                    }
                  >
                    <option value="SAFETY">
                      Safety
                    </option>

                    <option value="MATERIAL_DAMAGE">
                      Material damage
                    </option>

                    <option value="SITE_ISSUE">
                      Site issue
                    </option>

                    <option value="QUALITY">
                      Quality
                    </option>

                    <option value="DELAY">
                      Delay
                    </option>

                    <option value="EQUIPMENT">
                      Equipment
                    </option>
                  </select>
                </label>

                <label>
                  Severity
                  <select
                    value={
                      incidentSeverity
                    }
                    onChange={(event) =>
                      setIncidentSeverity(
                        event.target.value as IncidentSeverity
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
                  Title
                  <input
                    required
                    maxLength={250}
                    value={incidentTitle}
                    onChange={(event) =>
                      setIncidentTitle(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Description
                  <textarea
                    required
                    value={
                      incidentDescription
                    }
                    onChange={(event) =>
                      setIncidentDescription(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label className="siteCheckbox">
                  <input
                    type="checkbox"
                    checked={
                      incidentClientVisible
                    }
                    onChange={(event) =>
                      setIncidentClientVisible(
                        event.target
                          .checked
                      )
                    }
                  />
                  Client visible
                </label>

                <button type="submit">
                  Report incident
                </button>
              </form>
            )}
          </div>
        </section>

        <section
          className="siteOpsSection"
          id="progress-updates"
        >
          <div className="commercialSectionHeader">
            <div>
              <p>
                PROGRESS LOG
              </p>

              <h2>
                Progress updates
              </h2>
            </div>

            <span>
              {progressUpdates.length}
            </span>
          </div>

          <div className="siteOpsGrid">
            <div className="progressUpdateList">
              {progressUpdates.length
                === 0
                && (
                  <div className="emptyState">
                    No progress updates.
                  </div>
                )}

              {progressUpdates.map(
                (update) => (
                  <article
                    key={update.id}
                    className="progressUpdateCard"
                  >
                    <header>
                      <strong>
                        {
                          update
                            .progressPercentageSnapshot
                        }
                        %
                      </strong>

                      <span>
                        {
                          formatDate(
                            update
                              .createdAt
                          )
                        }
                      </span>
                    </header>

                    <p>
                      {update.summary}
                    </p>

                    {update.blockers && (
                      <div>
                        <span>
                          Blockers
                        </span>

                        <p>
                          {
                            update
                              .blockers
                          }
                        </p>
                      </div>
                    )}

                    {update.nextSteps && (
                      <div>
                        <span>
                          Next steps
                        </span>

                        <p>
                          {
                            update
                              .nextSteps
                          }
                        </p>
                      </div>
                    )}
                  </article>
                )
              )}
            </div>

            {canManage && (
              <form
                className="commercialForm siteMobileForm"
                onSubmit={
                  createProgressUpdate
                }
              >
                <h3>
                  Add progress update
                </h3>

                <p className="siteFormHint">
                  The percentage is captured automatically from the backend project progress calculation.
                </p>

                <label>
                  Summary
                  <textarea
                    required
                    value={
                      progressSummary
                    }
                    onChange={(event) =>
                      setProgressSummary(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Blockers
                  <textarea
                    value={
                      progressBlockers
                    }
                    onChange={(event) =>
                      setProgressBlockers(
                        event.target.value
                      )
                    }
                  />
                </label>

                <label>
                  Next steps
                  <textarea
                    value={
                      progressNextSteps
                    }
                    onChange={(event) =>
                      setProgressNextSteps(
                        event.target.value
                      )
                    }
                  />
                </label>

                <button type="submit">
                  Save progress update
                </button>
              </form>
            )}
          </div>
        </section>
      </section>
    </main>
  );
}
