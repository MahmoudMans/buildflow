import type {
  DailySiteReport,
  SitePhotoCategory,
} from "@/lib/site-operation-types";

// ---------------------------------------------------------------------------
// Limits (the same ones the server enforces)
// ---------------------------------------------------------------------------

export const CAPTURE_TEXT_MAX = 20000;
export const CAPTURE_WEATHER_MAX = 500;
export const CAPTURE_WORKERS_MAX = 100000;
export const CAPTION_MAX = 1000;
export const CAPTURE_MAX_PHOTOS = 20;

// ---------------------------------------------------------------------------
// Who may capture
// ---------------------------------------------------------------------------

// The same organization roles the site operations page lets manage reports.
// The server still checks the project role.
export function canCaptureSiteReport(role: string | null | undefined): boolean {
  return (
    role === "OWNER" ||
    role === "ADMIN" ||
    role === "PROJECT_MANAGER" ||
    role === "SITE_MANAGER"
  );
}

// ---------------------------------------------------------------------------
// Steps
// ---------------------------------------------------------------------------

export type CaptureStep = "basics" | "work" | "photos" | "review";

export const CAPTURE_STEPS: { key: CaptureStep; label: string; title: string }[] = [
  { key: "basics", label: "Basics", title: "The day on site" },
  { key: "work", label: "Work", title: "What happened today" },
  { key: "photos", label: "Photos", title: "Add photos" },
  { key: "review", label: "Send", title: "Check and send" },
];

export function stepIndex(step: CaptureStep): number {
  return CAPTURE_STEPS.findIndex((item) => item.key === step);
}

export function nextStep(step: CaptureStep): CaptureStep {
  return CAPTURE_STEPS[Math.min(stepIndex(step) + 1, CAPTURE_STEPS.length - 1)].key;
}

export function previousStep(step: CaptureStep): CaptureStep {
  return CAPTURE_STEPS[Math.max(stepIndex(step) - 1, 0)].key;
}

// ---------------------------------------------------------------------------
// Dates
// ---------------------------------------------------------------------------

const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];

// A real calendar day written as YYYY-MM-DD.
export function isValidDay(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }

  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));

  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  );
}

// Today's date where the organization is, not in UTC: a report started just
// after midnight in Tunis belongs to that Tunis day.
export function todayInTimeZone(now: Date, timeZone: string): string {
  try {
    const parts = new Intl.DateTimeFormat("en-CA", {
      timeZone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).formatToParts(now);

    const part = (type: string) => parts.find((item) => item.type === type)?.value;
    const year = part("year");
    const month = part("month");
    const day = part("day");

    if (year && month && day) {
      return `${year}-${month}-${day}`;
    }
  } catch {
    // An unknown time zone name: fall back to UTC below.
  }

  return now.toISOString().slice(0, 10);
}

// "Tue 6 Oct 2026"
export function formatReportDay(day: string): string {
  if (!isValidDay(day)) {
    return day;
  }

  const [year, month, date] = day.split("-").map(Number);
  const weekday = new Date(Date.UTC(year, month - 1, date)).getUTCDay();

  return `${WEEKDAYS[weekday]} ${date} ${MONTHS[month - 1]} ${year}`;
}

// "14:32" in the given time zone.
export function formatSavedTime(iso: string, timeZone: string): string {
  const time = new Date(iso);

  if (Number.isNaN(time.getTime())) {
    return "";
  }

  try {
    const parts = new Intl.DateTimeFormat("en-GB", {
      timeZone,
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    }).formatToParts(time);

    const hour = parts.find((item) => item.type === "hour")?.value;
    const minute = parts.find((item) => item.type === "minute")?.value;

    if (hour && minute) {
      return `${hour}:${minute}`;
    }
  } catch {
    // Unknown time zone: UTC below.
  }

  return time.toISOString().slice(11, 16);
}

// ---------------------------------------------------------------------------
// The report as it is typed
// ---------------------------------------------------------------------------

export interface CaptureDraft {
  reportDate: string;
  weatherText: string;
  // Kept as typed so the box can be empty while the person edits it.
  workersOnSite: string;
  summary: string;
  workCompleted: string;
  delays: string;
  issues: string;
  nextSteps: string;
}

export type CaptureField = keyof CaptureDraft;

export const WEATHER_CHOICES = ["Sunny", "Cloudy", "Rain", "Windy", "Hot", "Cold"];

export function emptyDraft(reportDate: string): CaptureDraft {
  return {
    reportDate,
    weatherText: "",
    workersOnSite: "",
    summary: "",
    workCompleted: "",
    delays: "",
    issues: "",
    nextSteps: "",
  };
}

export function draftFromReport(report: DailySiteReport): CaptureDraft {
  return {
    reportDate: report.reportDate,
    weatherText: report.weatherText ?? "",
    workersOnSite: String(report.workersOnSite),
    summary: report.summary,
    workCompleted: report.workCompleted ?? "",
    delays: report.delays ?? "",
    issues: report.issues ?? "",
    nextSteps: report.nextSteps ?? "",
  };
}

// Nothing worth keeping has been typed yet.
export function isDraftEmpty(draft: CaptureDraft): boolean {
  const workers = draft.workersOnSite.trim();

  return (
    (workers === "" || workers === "0") &&
    [
      draft.weatherText,
      draft.summary,
      draft.workCompleted,
      draft.delays,
      draft.issues,
      draft.nextSteps,
    ].every((value) => value.trim() === "")
  );
}

// A blank box counts as no workers. null means "not a whole number".
export function parseWorkers(value: string): number | null {
  const text = value.trim();

  if (text === "") {
    return 0;
  }

  if (!/^\d{1,9}$/.test(text)) {
    return null;
  }

  return Number(text);
}

// ---------------------------------------------------------------------------
// Checking what was typed
// ---------------------------------------------------------------------------

export type CaptureErrors = Partial<Record<CaptureField, string>>;

const TOO_LONG = "That is too long: please keep it under 20,000 characters.";

export function validateDraft(draft: CaptureDraft): CaptureErrors {
  const errors: CaptureErrors = {};

  if (!isValidDay(draft.reportDate)) {
    errors.reportDate = "Choose a valid date.";
  }

  if (draft.weatherText.trim().length > CAPTURE_WEATHER_MAX) {
    errors.weatherText = "Keep the weather note under 500 characters.";
  }

  const workers = parseWorkers(draft.workersOnSite);

  if (workers === null) {
    errors.workersOnSite = "Enter a whole number of workers, 0 or more.";
  } else if (workers > CAPTURE_WORKERS_MAX) {
    errors.workersOnSite = "That is more workers than can be recorded.";
  }

  if (draft.summary.trim() === "") {
    errors.summary = "Write a short summary of the day.";
  } else if (draft.summary.trim().length > CAPTURE_TEXT_MAX) {
    errors.summary = TOO_LONG;
  }

  for (const field of ["workCompleted", "delays", "issues", "nextSteps"] as const) {
    if (draft[field].trim().length > CAPTURE_TEXT_MAX) {
      errors[field] = TOO_LONG;
    }
  }

  return errors;
}

const FIELDS_BY_STEP: Record<CaptureStep, CaptureField[]> = {
  basics: ["reportDate", "weatherText", "workersOnSite"],
  work: ["summary", "workCompleted", "delays", "issues", "nextSteps"],
  photos: [],
  review: [],
};

// The step where a field is filled in.
export function stepForField(field: CaptureField): CaptureStep {
  return FIELDS_BY_STEP.basics.includes(field) ? "basics" : "work";
}

export function errorsForStep(step: CaptureStep, errors: CaptureErrors): CaptureErrors {
  const result: CaptureErrors = {};

  for (const field of FIELDS_BY_STEP[step]) {
    const message = errors[field];

    if (message) {
      result[field] = message;
    }
  }

  return result;
}

// The earliest step that has a problem, so "Send" can take the person there.
export function firstStepWithError(errors: CaptureErrors): CaptureStep | null {
  for (const step of CAPTURE_STEPS) {
    if (Object.keys(errorsForStep(step.key, errors)).length > 0) {
      return step.key;
    }
  }

  return null;
}

// ---------------------------------------------------------------------------
// What is sent to the server
// ---------------------------------------------------------------------------

function textOrNull(value: string): string | null {
  const text = value.trim();

  return text === "" ? null : text;
}

export interface CreateReportBody {
  reportDate: string;
  weatherText: string | null;
  workersOnSite: number;
  summary: string;
  workCompleted: string | null;
  delays: string | null;
  issues: string | null;
  nextSteps: string | null;
}

export function buildCreateRequest(draft: CaptureDraft): CreateReportBody {
  return {
    reportDate: draft.reportDate,
    weatherText: textOrNull(draft.weatherText),
    workersOnSite: parseWorkers(draft.workersOnSite) ?? 0,
    summary: draft.summary.trim(),
    workCompleted: textOrNull(draft.workCompleted),
    delays: textOrNull(draft.delays),
    issues: textOrNull(draft.issues),
    nextSteps: textOrNull(draft.nextSteps),
  };
}

export interface UpdateReportBody {
  weatherText: string;
  workersOnSite: number;
  summary: string;
  workCompleted: string;
  delays: string;
  issues: string;
  nextSteps: string;
}

// The server leaves a field alone when it is null, so a cleared box is sent
// as an empty string, which the server stores as nothing.
export function buildUpdateRequest(draft: CaptureDraft): UpdateReportBody {
  return {
    weatherText: draft.weatherText.trim(),
    workersOnSite: parseWorkers(draft.workersOnSite) ?? 0,
    summary: draft.summary.trim(),
    workCompleted: draft.workCompleted.trim(),
    delays: draft.delays.trim(),
    issues: draft.issues.trim(),
    nextSteps: draft.nextSteps.trim(),
  };
}

// ---------------------------------------------------------------------------
// Saving the draft on this phone
// ---------------------------------------------------------------------------

export interface StorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

export interface StoredDraft {
  savedAt: string;
  draft: CaptureDraft;
}

export function draftKey(organizationId: string, projectId: string, reportDate: string): string {
  return `buildflow.siteReportDraft.${organizationId}.${projectId}.${reportDate}`;
}

function asText(value: unknown): string {
  return typeof value === "string" ? value : "";
}

export function readDraft(storage: StorageLike | null, key: string): StoredDraft | null {
  if (!storage) {
    return null;
  }

  try {
    const raw = storage.getItem(key);

    if (!raw) {
      return null;
    }

    const parsed: unknown = JSON.parse(raw);

    if (typeof parsed !== "object" || parsed === null) {
      return null;
    }

    const record = parsed as Record<string, unknown>;
    const source = record.draft;

    if (typeof source !== "object" || source === null) {
      return null;
    }

    const fields = source as Record<string, unknown>;
    const reportDate = asText(fields.reportDate);

    if (!isValidDay(reportDate)) {
      return null;
    }

    return {
      savedAt: asText(record.savedAt),
      draft: {
        reportDate,
        weatherText: asText(fields.weatherText),
        workersOnSite: asText(fields.workersOnSite),
        summary: asText(fields.summary),
        workCompleted: asText(fields.workCompleted),
        delays: asText(fields.delays),
        issues: asText(fields.issues),
        nextSteps: asText(fields.nextSteps),
      },
    };
  } catch {
    return null;
  }
}

// Returns false when the phone would not keep it (private mode, full storage).
export function writeDraft(
  storage: StorageLike | null,
  key: string,
  draft: CaptureDraft,
  now: Date,
): boolean {
  if (!storage) {
    return false;
  }

  try {
    storage.setItem(key, JSON.stringify({ savedAt: now.toISOString(), draft }));

    return true;
  } catch {
    return false;
  }
}

export function removeDraft(storage: StorageLike | null, key: string): void {
  if (!storage) {
    return;
  }

  try {
    storage.removeItem(key);
  } catch {
    // Nothing more to do.
  }
}

// ---------------------------------------------------------------------------
// Photos
// ---------------------------------------------------------------------------

export type PhotoStatus = "ready" | "uploading" | "done" | "failed";

export interface CapturePhoto {
  id: string;
  fileName: string;
  mimeType: string;
  // The picture as a data URL: used for the preview and for the upload.
  dataUrl: string;
  sizeBytes: number;
  category: SitePhotoCategory;
  caption: string;
  clientVisible: boolean;
  status: PhotoStatus;
  error: string;
}

export const PHOTO_CATEGORIES: { value: SitePhotoCategory; label: string }[] = [
  { value: "BEFORE", label: "Before work" },
  { value: "PROGRESS", label: "Progress" },
  { value: "AFTER", label: "Completed" },
  { value: "DELIVERY", label: "Delivery" },
  { value: "DAMAGE", label: "Damage" },
  { value: "SAFETY_ISSUE", label: "Safety issue" },
  { value: "OTHER", label: "Other" },
];

export const DEFAULT_PHOTO_CATEGORY: SitePhotoCategory = "PROGRESS";

export function photoCategoryLabel(category: SitePhotoCategory): string {
  return PHOTO_CATEGORIES.find((item) => item.value === category)?.label ?? "Other";
}

export function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export interface PhotoBody {
  fileName: string;
  mimeType: string;
  base64Data: string;
  caption: string | null;
  category: SitePhotoCategory;
  clientVisible: boolean;
}

export function buildPhotoRequest(photo: CapturePhoto): PhotoBody {
  return {
    fileName: photo.fileName,
    mimeType: photo.mimeType,
    base64Data: photo.dataUrl,
    caption: textOrNull(photo.caption),
    category: photo.category,
    clientVisible: photo.clientVisible,
  };
}

// Photos that still have to be sent: new ones and ones that failed before.
export function pendingPhotos(photos: CapturePhoto[]): CapturePhoto[] {
  return photos.filter((photo) => photo.status === "ready" || photo.status === "failed");
}

export function photoSummary(photos: CapturePhoto[]): string {
  const total = photos.length;

  if (total === 0) {
    return "No photos added";
  }

  const done = photos.filter((photo) => photo.status === "done").length;
  const failed = photos.filter((photo) => photo.status === "failed").length;
  const noun = total === 1 ? "photo" : "photos";

  if (done === total) {
    return total === 1 ? "The photo was sent" : `All ${total} photos were sent`;
  }

  if (done > 0 || failed > 0) {
    return `${done} of ${total} ${noun} sent, ${total - done} not sent yet`;
  }

  return `${total} ${noun} ready to send`;
}

// A short, readable message for whatever went wrong.
export function errorMessage(caught: unknown, fallback: string): string {
  if (caught instanceof TypeError) {
    return "There is no connection right now. Check your signal and try again.";
  }

  if (caught instanceof Error && caught.message.trim() !== "") {
    return caught.message;
  }

  if (typeof caught === "object" && caught !== null && "message" in caught) {
    const message = (caught as { message: unknown }).message;

    if (typeof message === "string" && message.trim() !== "") {
      return message;
    }
  }

  return fallback;
}

// Sends the photos that are waiting, one after the other, and carries on when
// one fails so a single bad photo never blocks the rest.
export async function uploadPending(
  photos: CapturePhoto[],
  send: (photo: CapturePhoto) => Promise<void>,
  onStatus: (id: string, status: PhotoStatus, error: string) => void,
): Promise<{ sent: number; failed: number }> {
  let sent = 0;
  let failed = 0;

  for (const photo of pendingPhotos(photos)) {
    onStatus(photo.id, "uploading", "");

    try {
      await send(photo);
      onStatus(photo.id, "done", "");
      sent += 1;
    } catch (caught) {
      onStatus(photo.id, "failed", errorMessage(caught, "This photo could not be sent."));
      failed += 1;
    }
  }

  return { sent, failed };
}
