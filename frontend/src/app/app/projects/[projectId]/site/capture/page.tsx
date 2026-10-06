"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { useAuth } from "@/components/AuthProvider";
import { BasicsStep, WorkStep } from "@/components/SiteCaptureFields";
import { SiteCapturePhotos } from "@/components/SiteCapturePhotos";
import { SiteCaptureReview } from "@/components/SiteCaptureReview";
import { SiteCaptureStepper } from "@/components/SiteCaptureStepper";
import { useOnlineStatus } from "@/components/useOnlineStatus";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";
import { EmptyState } from "@/components/ux/EmptyState";
import { ErrorState } from "@/components/ux/ErrorState";

import { ApiClientError, apiGet, apiPost } from "@/lib/api";
import {
  CAPTURE_MAX_PHOTOS,
  CAPTURE_STEPS,
  DEFAULT_PHOTO_CATEGORY,
  buildCreateRequest,
  buildPhotoRequest,
  buildUpdateRequest,
  canCaptureSiteReport,
  draftFromReport,
  draftKey,
  emptyDraft,
  errorMessage,
  errorsForStep,
  firstStepWithError,
  formatReportDay,
  formatSavedTime,
  isDraftEmpty,
  isValidDay,
  nextStep,
  pendingPhotos,
  previousStep,
  readDraft,
  removeDraft,
  stepIndex,
  todayInTimeZone,
  uploadPending,
  validateDraft,
  writeDraft,
} from "@/lib/site-capture";
import type {
  CaptureDraft,
  CapturePhoto,
  CaptureStep,
  PhotoStatus,
  StorageLike,
} from "@/lib/site-capture";
import type { DailySiteReport } from "@/lib/site-operation-types";
import { prepareBrowserPhoto } from "@/lib/site-photo";

type CaptureProject = { name: string };

type Phase = "editing" | "sending" | "partial" | "done";

// The phone's own storage, or nothing when the browser will not give it
// (private mode, blocked site data).
function browserStorage(): StorageLike | null {
  try {
    return typeof window === "undefined" ? null : window.localStorage;
  } catch {
    return null;
  }
}

// The form for one project in one organization. The page gives it a key, so
// starting over or switching project begins afresh.
function CapturePanel({
  organizationId,
  projectId,
  timeZone,
  onRestart,
}: {
  organizationId: string;
  projectId: string;
  timeZone: string;
  onRestart: () => void;
}) {
  const [today] = useState(() => todayInTimeZone(new Date(), timeZone));
  const [step, setStep] = useState<CaptureStep>("basics");
  const [draft, setDraft] = useState<CaptureDraft>(() => emptyDraft(today));
  const [dirty, setDirty] = useState(false);
  const [photos, setPhotos] = useState<CapturePhoto[]>([]);
  const [preparing, setPreparing] = useState(0);
  const [photoNotice, setPhotoNotice] = useState("");
  const [project, setProject] = useState<CaptureProject | null>(null);
  const [reports, setReports] = useState<DailySiteReport[] | null>(null);
  const [loadError, setLoadError] = useState("");
  const [restoredAt, setRestoredAt] = useState<string | null>(null);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [showErrors, setShowErrors] = useState(false);
  const [phase, setPhase] = useState<Phase>("editing");
  const [retrying, setRetrying] = useState(false);
  const [sendError, setSendError] = useState("");
  const [savedReportId, setSavedReportId] = useState<string | null>(null);

  const online = useOnlineStatus();
  const photoCounter = useRef(0);
  const headingRef = useRef<HTMLHeadingElement>(null);

  // What to show for a day: an unsent draft from this phone first, then the
  // report already on the server, then a blank form.
  const draftForDay = useCallback(
    (day: string, list: DailySiteReport[]) => {
      const stored = readDraft(browserStorage(), draftKey(organizationId, projectId, day));

      if (stored && !isDraftEmpty(stored.draft)) {
        return { draft: stored.draft, restoredAt: stored.savedAt || null };
      }

      const existing = list.find((report) => report.reportDate === day);

      if (existing) {
        return { draft: draftFromReport(existing), restoredAt: null };
      }

      return { draft: emptyDraft(day), restoredAt: null };
    },
    [organizationId, projectId],
  );

  useEffect(() => {
    let cancelled = false;

    const timer = window.setTimeout(() => {
      Promise.all([
        apiGet<CaptureProject>(`/projects/${projectId}`, organizationId),
        apiGet<DailySiteReport[]>(`/projects/${projectId}/site-reports`, organizationId),
      ])
        .then(([projectResult, reportResult]) => {
          if (cancelled) {
            return;
          }

          const start = draftForDay(today, reportResult);

          setProject(projectResult);
          setReports(reportResult);
          setDraft(start.draft);
          setRestoredAt(start.restoredAt);
        })
        .catch((caught: unknown) => {
          if (!cancelled) {
            setLoadError(errorMessage(caught, "Could not load this project."));
          }
        });
    }, 0);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [organizationId, projectId, today, draftForDay]);

  // Keep what is typed on this phone, a moment after the typing stops.
  useEffect(() => {
    if (!dirty || phase !== "editing" || !isValidDay(draft.reportDate)) {
      return;
    }

    const key = draftKey(organizationId, projectId, draft.reportDate);

    const timer = window.setTimeout(() => {
      const storage = browserStorage();

      if (isDraftEmpty(draft)) {
        removeDraft(storage, key);
        setSavedAt(null);

        return;
      }

      const now = new Date();

      if (writeDraft(storage, key, draft, now)) {
        setSavedAt(now.toISOString());
      }
    }, 600);

    return () => {
      window.clearTimeout(timer);
    };
  }, [dirty, draft, phase, organizationId, projectId]);

  const errors = validateDraft(draft);
  const existingReport = reports?.find((report) => report.reportDate === draft.reportDate) ?? null;
  const current = CAPTURE_STEPS[stepIndex(step)];

  function showStep(next: CaptureStep) {
    setStep(next);

    window.setTimeout(() => {
      headingRef.current?.focus();
      headingRef.current?.scrollIntoView?.({ block: "start" });
    }, 0);
  }

  function changeDay(day: string) {
    // Keep what was typed for the day being left.
    if (dirty && isValidDay(draft.reportDate) && !isDraftEmpty(draft)) {
      writeDraft(
        browserStorage(),
        draftKey(organizationId, projectId, draft.reportDate),
        draft,
        new Date(),
      );
    }

    const next = draftForDay(day, reports ?? []);

    setDraft(next.draft);
    setRestoredAt(next.restoredAt);
    setSavedAt(null);
    setDirty(false);
    setShowErrors(false);
  }

  function handleChange(patch: Partial<CaptureDraft>) {
    if (
      patch.reportDate !== undefined &&
      patch.reportDate !== draft.reportDate &&
      isValidDay(patch.reportDate)
    ) {
      changeDay(patch.reportDate);

      return;
    }

    setDraft((previous) => ({ ...previous, ...patch }));
    setDirty(true);
  }

  function startOver() {
    removeDraft(browserStorage(), draftKey(organizationId, projectId, draft.reportDate));

    const existing = reports?.find((report) => report.reportDate === draft.reportDate);

    setDraft(existing ? draftFromReport(existing) : emptyDraft(draft.reportDate));
    setRestoredAt(null);
    setSavedAt(null);
    setDirty(false);
  }

  function goNext() {
    if (Object.keys(errorsForStep(step, errors)).length > 0) {
      setShowErrors(true);

      return;
    }

    setShowErrors(false);
    showStep(nextStep(step));
  }

  async function addFiles(files: File[]) {
    const room = Math.max(0, CAPTURE_MAX_PHOTOS - photos.length - preparing);
    const accepted = files.slice(0, room);
    const notices: string[] = [];

    if (files.length > accepted.length) {
      notices.push(`Only ${CAPTURE_MAX_PHOTOS} photos can go in one report.`);
    }

    setPhotoNotice("");
    setPreparing((count) => count + accepted.length);

    for (const file of accepted) {
      try {
        const prepared = await prepareBrowserPhoto(file);

        photoCounter.current += 1;

        const photo: CapturePhoto = {
          id: `photo-${photoCounter.current}`,
          fileName: prepared.fileName,
          mimeType: prepared.mimeType,
          dataUrl: prepared.dataUrl,
          sizeBytes: prepared.sizeBytes,
          category: DEFAULT_PHOTO_CATEGORY,
          caption: "",
          clientVisible: false,
          status: "ready",
          error: "",
        };

        setPhotos((previous) => [...previous, photo]);
      } catch (caught) {
        notices.push(`${file.name || "A photo"}: ${errorMessage(caught, "could not be added.")}`);
      } finally {
        setPreparing((count) => count - 1);
      }
    }

    setPhotoNotice(notices.join(" "));
  }

  function removePhoto(id: string) {
    setPhotos((previous) => previous.filter((photo) => photo.id !== id));
  }

  function changePhoto(
    id: string,
    patch: Partial<Pick<CapturePhoto, "category" | "caption" | "clientVisible">>,
  ) {
    setPhotos((previous) =>
      previous.map((photo) => (photo.id === id ? { ...photo, ...patch } : photo)),
    );
  }

  function setPhotoStatus(id: string, status: PhotoStatus, error: string) {
    setPhotos((previous) =>
      previous.map((photo) => (photo.id === id ? { ...photo, status, error } : photo)),
    );
  }

  function updateExisting(reportId: string): Promise<DailySiteReport> {
    return apiPost<DailySiteReport>(
      `/site-reports/${reportId}/update`,
      buildUpdateRequest(draft),
      organizationId,
    );
  }

  // Updates the report of that day when there is one, otherwise creates it.
  async function saveReport(): Promise<string> {
    if (existingReport) {
      return (await updateExisting(existingReport.id)).id;
    }

    try {
      const created = await apiPost<DailySiteReport>(
        `/projects/${projectId}/site-reports`,
        buildCreateRequest(draft),
        organizationId,
      );

      return created.id;
    } catch (caught) {
      // Someone else made today's report in the meantime: add to theirs.
      if (caught instanceof ApiClientError && caught.code === "DAILY_SITE_REPORT_ALREADY_EXISTS") {
        const list = await apiGet<DailySiteReport[]>(
          `/projects/${projectId}/site-reports`,
          organizationId,
        );

        setReports(list);

        const found = list.find((report) => report.reportDate === draft.reportDate);

        if (found) {
          return (await updateExisting(found.id)).id;
        }
      }

      throw caught;
    }
  }

  function sendPhotos(reportId: string, list: CapturePhoto[]) {
    return uploadPending(
      list,
      async (photo) => {
        await apiPost(
          `/site-reports/${reportId}/photos`,
          buildPhotoRequest(photo),
          organizationId,
        );
      },
      setPhotoStatus,
    );
  }

  async function send() {
    if (phase === "sending") {
      return;
    }

    const problem = firstStepWithError(errors);

    if (problem) {
      setShowErrors(true);
      showStep(problem);

      return;
    }

    setPhase("sending");
    setSendError("");

    try {
      let reportId = savedReportId;

      if (!reportId) {
        reportId = await saveReport();

        setSavedReportId(reportId);
        setDirty(false);
        setSavedAt(null);
        removeDraft(browserStorage(), draftKey(organizationId, projectId, draft.reportDate));
      }

      const result = await sendPhotos(reportId, photos);

      setPhase(result.failed > 0 ? "partial" : "done");
    } catch (caught) {
      setSendError(errorMessage(caught, "The report could not be sent."));
      setPhase("editing");
    }
  }

  async function retryPhotos() {
    if (!savedReportId || retrying) {
      return;
    }

    setRetrying(true);

    const result = await sendPhotos(savedReportId, photos);

    setRetrying(false);

    if (result.failed === 0) {
      setPhase("done");
    }
  }

  if (loadError) {
    return (
      <ErrorState title="Could not load the project" message={loadError} onRetry={onRestart} />
    );
  }

  if (!reports) {
    return (
      <p className="uxCaptureLoading" role="status">
        Loading the project...
      </p>
    );
  }

  const backLink = (
    <Link className="uxCaptureBack" href={`/app/projects/${projectId}/site`}>
      ← Site operations
    </Link>
  );

  if (phase === "done") {
    return (
      <section className="uxCaptureDone" aria-label="Report sent">
        <div className="uxCaptureDoneIcon" aria-hidden="true">
          ✓
        </div>

        <h2>Report sent</h2>

        <p>
          The site report for {formatReportDay(draft.reportDate)} is saved
          {photos.length > 0
            ? ` with ${photos.length} ${photos.length === 1 ? "photo" : "photos"}`
            : ""}
          .
        </p>

        <div className="uxCaptureActions">
          <Link className="uxCaptureLinkButton" href={`/app/projects/${projectId}/site`}>
            View site operations
          </Link>

          <button type="button" className="uxPrimaryButton uxCaptureBig" onClick={onRestart}>
            Report another day
          </button>
        </div>
      </section>
    );
  }

  if (phase === "partial") {
    const waiting = pendingPhotos(photos).length;

    return (
      <section className="uxCaptureDone partial" aria-label="Some photos were not sent">
        <h2>The report is saved, but some photos did not send</h2>

        <p>
          {waiting === 1 ? "1 photo is" : `${waiting} photos are`} still on this phone. Keep this
          page open and try again when your signal is better.
        </p>

        <ul className="uxCapturePartialList">
          {photos.map((photo, index) => (
            <li key={photo.id}>
              <span>
                Photo {index + 1}: {photo.fileName}
              </span>

              <span
                className={`uxStatusBadge uxCapturePhotoStatus ${
                  photo.status === "done" ? "success" : photo.status === "uploading" ? "info" : "danger"
                }`}
              >
                <span className="uxStatusDot" aria-hidden="true" />
                {photo.status === "done"
                  ? "Sent"
                  : photo.status === "uploading"
                    ? "Sending..."
                    : "Not sent"}
              </span>

              {photo.error ? <small className="uxFieldError">{photo.error}</small> : null}
            </li>
          ))}
        </ul>

        <div className="uxCaptureActions">
          <Link className="uxCaptureLinkButton" href={`/app/projects/${projectId}/site`}>
            View site operations
          </Link>

          <button
            type="button"
            className="uxPrimaryButton uxCaptureBig"
            disabled={retrying || !online}
            onClick={() => {
              void retryPhotos();
            }}
          >
            {retrying ? "Sending..." : "Try sending again"}
          </button>
        </div>

        {!online ? (
          <p className="uxCaptureHint">You are offline. Try again when your signal is back.</p>
        ) : null}
      </section>
    );
  }

  const sending = phase === "sending";
  const uploading = photos.filter((photo) => photo.status === "uploading").length > 0;
  const sentCount = photos.filter((photo) => photo.status === "done").length;

  const sendingLabel = uploading
    ? `Sending photo ${Math.min(sentCount + 1, photos.length)} of ${photos.length}...`
    : "Saving the report...";

  const shownErrors = showErrors ? errors : {};

  return (
    <div className="uxCapture">
      {backLink}

      {!online ? (
        <p className="uxCaptureBanner warning" role="status">
          You are offline. What you type is kept on this phone. Sending works again when your
          signal is back.
        </p>
      ) : null}

      {restoredAt ? (
        <p className="uxCaptureBanner" role="status">
          Restored your unsent draft{formatSavedTime(restoredAt, timeZone)
            ? ` from ${formatSavedTime(restoredAt, timeZone)}`
            : ""}
          .{" "}
          <button type="button" className="uxLinkButton" onClick={startOver}>
            Start over
          </button>
        </p>
      ) : null}

      {existingReport ? (
        <p className="uxCaptureBanner" role="status">
          A report for {formatReportDay(existingReport.reportDate)} already exists. Sending will
          update it{restoredAt ? " with this draft." : " and add any photos."}
        </p>
      ) : null}

      <SiteCaptureStepper current={step} onGoTo={showStep} />

      <p className="uxCaptureProgress">
        Step {stepIndex(step) + 1} of {CAPTURE_STEPS.length}
        {project ? ` · ${project.name}` : ""}
      </p>

      <h2 className="uxCaptureTitle" ref={headingRef} tabIndex={-1}>
        {current.title}
      </h2>

      <div className="uxCaptureBody">
        {step === "basics" ? (
          <BasicsStep draft={draft} errors={errorsForStep("basics", shownErrors)} today={today} onChange={handleChange} />
        ) : null}

        {step === "work" ? (
          <WorkStep draft={draft} errors={errorsForStep("work", shownErrors)} onChange={handleChange} />
        ) : null}

        {step === "photos" ? (
          <SiteCapturePhotos
            photos={photos}
            preparing={preparing}
            notice={photoNotice}
            maxPhotos={CAPTURE_MAX_PHOTOS}
            locked={sending}
            onAddFiles={(files) => {
              void addFiles(files);
            }}
            onRemove={removePhoto}
            onChange={changePhoto}
          />
        ) : null}

        {step === "review" ? (
          <SiteCaptureReview draft={draft} photos={photos} errors={errors} onEdit={showStep} />
        ) : null}
      </div>

      {sendError ? (
        <p className="uxFieldError uxCaptureSendError" role="alert">
          {sendError}
        </p>
      ) : null}

      {sending ? (
        <p className="uxCaptureHint" role="status">
          {sendingLabel}
        </p>
      ) : null}

      <div className="uxCaptureActions">
        {step !== "basics" ? (
          <button
            type="button"
            className="uxSecondaryButton uxCaptureBig"
            disabled={sending}
            onClick={() => showStep(previousStep(step))}
          >
            Back
          </button>
        ) : null}

        {step === "review" ? (
          <button
            type="button"
            className="uxPrimaryButton uxCaptureBig"
            disabled={sending || !online || preparing > 0}
            onClick={() => {
              void send();
            }}
          >
            {sending ? "Sending..." : "Send report"}
          </button>
        ) : (
          <button
            type="button"
            className="uxPrimaryButton uxCaptureBig"
            disabled={sending}
            onClick={goNext}
          >
            {step === "photos" && photos.length === 0 ? "Skip photos" : "Next"}
          </button>
        )}
      </div>

      {step === "review" && !online ? (
        <p className="uxCaptureHint">You are offline, so the report cannot be sent yet.</p>
      ) : null}

      <p className="uxCaptureSaved" role="status">
        {savedAt
          ? `Draft kept on this phone at ${formatSavedTime(savedAt, timeZone)}`
          : ""}
      </p>
    </div>
  );
}

export default function SiteCapturePage() {
  const params = useParams();
  const router = useRouter();

  const projectId = String(params.projectId);

  const { user, organizations, loading } = useAuth();

  const organizationSelection = useOrganizationSelection(organizations);
  const organizationId = organizationSelection.organizationId;
  const organization = organizations.find((item) => item.id === organizationId);

  // Bumped to start a fresh report (or retry loading).
  const [round, setRound] = useState(0);

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, user, router]);

  if (loading || !user) {
    return <main className="appLoading">Loading...</main>;
  }

  return (
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent uxCapturePage">
        <header className="appHeader">
          <div>
            <p>{organization?.name ?? "Organization"}</p>

            <h1>Quick site report</h1>

            <span className="projectClientName">One step at a time, made for your phone</span>
          </div>
        </header>

        {!organization && (
          <EmptyState
            title="No organization selected"
            description="Choose an organization to add a site report."
          />
        )}

        {organization && !canCaptureSiteReport(organization.role) && (
          <EmptyState
            title="Site reports are added by site and project managers"
            description={`Ask a site manager, project manager or administrator of ${organization.name} to add one, or to change your role.`}
          />
        )}

        {organization && canCaptureSiteReport(organization.role) && (
          <CapturePanel
            key={`${organization.id}:${projectId}:${round}`}
            organizationId={organization.id}
            projectId={projectId}
            timeZone={organization.timezone}
            onRestart={() => setRound((value) => value + 1)}
          />
        )}
      </section>
    </main>
  );
}
