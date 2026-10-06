import { isCanonicalUuid } from "@/lib/ux-validation";
import type { StatusTone } from "@/lib/ux-validation";
import type {
  AuditActorOption,
  AuditEntry,
  AuditFacets,
  AuditFilters,
  AuditPage,
  AuditProjectOption,
} from "@/lib/audit-types";

export const AUDIT_PAGE_SIZE = 25;

export const AUDIT_TEXT_MAX = 100;

export const EMPTY_AUDIT_FILTERS: AuditFilters = {
  q: "",
  projectId: "",
  actorUserId: "",
  entityType: "",
  action: "",
  from: "",
  to: "",
};

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

// ---------------------------------------------------------------------------
// Who may open the audit log
// ---------------------------------------------------------------------------

// The same rule the server applies: owners and administrators only.
export function canViewAuditLog(role: string | null | undefined): boolean {
  return role === "OWNER" || role === "ADMIN";
}

// ---------------------------------------------------------------------------
// Filters
// ---------------------------------------------------------------------------

export function isIsoDay(value: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

  if (!match) {
    return false;
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(Date.UTC(year, month - 1, day));

  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  );
}

export function hasActiveFilters(filters: AuditFilters): boolean {
  return Object.values(filters).some((value) => value.trim() !== "");
}

// A message for the person, or null when the filters can be sent.
export function validateAuditFilters(filters: AuditFilters): string | null {
  if (filters.from && !isIsoDay(filters.from)) {
    return "The start date is not a valid date.";
  }

  if (filters.to && !isIsoDay(filters.to)) {
    return "The end date is not a valid date.";
  }

  if (filters.from && filters.to && filters.from > filters.to) {
    return "The start date must be on or before the end date.";
  }

  return null;
}

// The query string for GET /audit/search. Empty or malformed values are left
// out rather than sent.
export function buildAuditQuery(
  filters: AuditFilters,
  page: number,
  size: number = AUDIT_PAGE_SIZE,
): string {
  const params = new URLSearchParams();

  params.set("page", String(Number.isFinite(page) ? Math.max(0, Math.floor(page)) : 0));
  params.set(
    "size",
    String(Number.isFinite(size) ? Math.min(100, Math.max(1, Math.floor(size))) : AUDIT_PAGE_SIZE),
  );

  const text = filters.q.trim().slice(0, AUDIT_TEXT_MAX);

  if (text) {
    params.set("q", text);
  }

  if (isCanonicalUuid(filters.projectId)) {
    params.set("projectId", filters.projectId.trim());
  }

  if (isCanonicalUuid(filters.actorUserId)) {
    params.set("actorUserId", filters.actorUserId.trim());
  }

  if (filters.entityType.trim()) {
    params.set("entityType", filters.entityType.trim());
  }

  if (filters.action.trim()) {
    params.set("action", filters.action.trim());
  }

  if (isIsoDay(filters.from)) {
    params.set("from", filters.from);
  }

  if (isIsoDay(filters.to)) {
    params.set("to", filters.to);
  }

  return `?${params.toString()}`;
}

// ---------------------------------------------------------------------------
// Words
// ---------------------------------------------------------------------------

function sentenceCase(value: string): string {
  const words = value.trim().replace(/[_-]+/g, " ").replace(/\s+/g, " ").toLowerCase();

  return words ? words.charAt(0).toUpperCase() + words.slice(1) : "";
}

// CHANGE_ORDER_CREATED -> "Change order created"
export function humanizeAction(action: string): string {
  return sentenceCase(action) || "Unknown action";
}

// SITE_REPORT -> "Site report"
export function humanizeEntityType(entityType: string): string {
  return sentenceCase(entityType) || "Record";
}

const SUCCESS_WORDS = new Set([
  "APPROVED",
  "RESOLVED",
  "COMPLETED",
  "ACCEPTED",
  "PAID",
  "CLOSED",
  "VERIFIED",
  "RECEIVED",
  "ACTIVATED",
  "DONE",
]);

const DANGER_WORDS = new Set([
  "DELETED",
  "REMOVED",
  "CANCELLED",
  "CANCELED",
  "REJECTED",
  "REVOKED",
  "VOIDED",
  "DISABLED",
  "FAILED",
  "DECLINED",
]);

const WARNING_WORDS = new Set([
  "SUBMITTED",
  "REQUESTED",
  "PENDING",
  "REOPENED",
  "ESCALATED",
]);

const INFO_WORDS = new Set([
  "CREATED",
  "ADDED",
  "REGISTERED",
  "INVITED",
  "UPLOADED",
  "SENT",
  "ISSUED",
  "ORDERED",
  "OPENED",
  "LOGGED",
  "STARTED",
]);

// The colour of an action's badge, decided by its last word.
export function actionTone(action: string): StatusTone {
  const words = action
    .trim()
    .toUpperCase()
    .split(/[_\s-]+/)
    .filter(Boolean);

  const last = words[words.length - 1] ?? "";

  if (SUCCESS_WORDS.has(last)) {
    return "success";
  }

  if (DANGER_WORDS.has(last)) {
    return "danger";
  }

  if (WARNING_WORDS.has(last)) {
    return "warning";
  }

  if (INFO_WORDS.has(last)) {
    return "info";
  }

  return "neutral";
}

// ---------------------------------------------------------------------------
// People and projects
// ---------------------------------------------------------------------------

type ActorFields = Pick<AuditEntry, "actorUserId" | "actorName" | "actorEmail">;

export function actorLabel(entry: ActorFields): string {
  return (
    entry.actorName?.trim() ||
    entry.actorEmail?.trim() ||
    (entry.actorUserId ? "Unknown person" : "System")
  );
}

// The email under the name, when there is a name to put it under.
export function actorDetail(entry: ActorFields): string | null {
  const name = entry.actorName?.trim();
  const email = entry.actorEmail?.trim();

  if (name && email) {
    return email;
  }

  if (!name && !email && !entry.actorUserId) {
    return "No signed-in user";
  }

  return null;
}

export function actorOptionLabel(actor: AuditActorOption): string {
  const name = actor.name?.trim();

  return name ? `${name} (${actor.email})` : actor.email;
}

export function projectOptionLabel(project: AuditProjectOption): string {
  return `${project.projectNumber} · ${project.name}`;
}

// ---------------------------------------------------------------------------
// Times
// ---------------------------------------------------------------------------

export interface AuditTime {
  date: string;
  time: string;
  label: string;
}

const NO_TIME: AuditTime = { date: "—", time: "", label: "—" };

function partsIn(date: Date, timeZone: string): Record<string, string> {
  const format = (zone: string) =>
    new Intl.DateTimeFormat("en-GB", {
      timeZone: zone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hourCycle: "h23",
    }).formatToParts(date);

  let parts: Intl.DateTimeFormatPart[];

  try {
    parts = format(timeZone || "UTC");
  } catch {
    parts = format("UTC");
  }

  const result: Record<string, string> = {};

  for (const part of parts) {
    result[part.type] = part.value;
  }

  return result;
}

// "11 Mar 2026" and "00:30:05" for an instant, read in the given time zone
// (the organization's, so it agrees with the date filters).
export function formatAuditTime(
  iso: string | null | undefined,
  timeZone: string,
): AuditTime {
  if (!iso) {
    return NO_TIME;
  }

  const date = new Date(iso);

  if (Number.isNaN(date.getTime())) {
    return NO_TIME;
  }

  const parts = partsIn(date, timeZone);
  const month = MONTHS[Number(parts.month) - 1] ?? parts.month;
  const day = `${parts.day} ${month} ${parts.year}`;
  const time = `${parts.hour}:${parts.minute}:${parts.second}`;

  return { date: day, time, label: `${day}, ${time}` };
}

// "2026-03-11" -> "11 Mar 2026"
export function formatDay(iso: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);

  if (!match) {
    return iso;
  }

  return `${match[3]} ${MONTHS[Number(match[2]) - 1] ?? match[2]} ${match[1]}`;
}

// ---------------------------------------------------------------------------
// The recorded values
// ---------------------------------------------------------------------------

export interface AuditField {
  key: string;
  label: string;
  value: string;
}

export type AuditValues =
  | { kind: "none" }
  | { kind: "fields"; typeName: string | null; fields: AuditField[] }
  | { kind: "text"; text: string };

function labelFromKey(key: string): string {
  const words = key
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/[_.]+/g, " ")
    .trim()
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .map((word) => (word === "id" ? "ID" : word));

  if (words.length === 0) {
    return key;
  }

  return words
    .map((word, index) =>
      index === 0 && word !== "ID"
        ? word.charAt(0).toUpperCase() + word.slice(1)
        : word,
    )
    .join(" ");
}

function field(key: string, rawValue: string): AuditField {
  const value = rawValue.trim();

  return { key, label: labelFromKey(key), value: value === "null" || value === "" ? "—" : value };
}

// Splits on commas that are not inside brackets.
function splitTopLevel(text: string): string[] {
  const parts: string[] = [];
  let depth = 0;
  let current = "";

  for (const character of text) {
    if (character === "[" || character === "{" || character === "(") {
      depth += 1;
    } else if (character === "]" || character === "}" || character === ")") {
      depth = Math.max(0, depth - 1);
    }

    if (character === "," && depth === 0) {
      parts.push(current);
      current = "";
    } else {
      current += character;
    }
  }

  if (current.trim() !== "") {
    parts.push(current);
  }

  return parts;
}

// TaskView[id=7, title=Pour slab, status=TODO]
function parseRecord(text: string): { typeName: string; fields: AuditField[] } | null {
  const match = /^([A-Za-z_$][\w.$]*)\[([\s\S]*)\]$/.exec(text);

  if (!match) {
    return null;
  }

  const fields: AuditField[] = [];

  for (const part of splitTopLevel(match[2])) {
    const equals = part.indexOf("=");
    const key = equals > 0 ? part.slice(0, equals).trim() : "";

    if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) {
      fields.push(field(key, part.slice(equals + 1)));
      continue;
    }

    // A comma inside a value, such as "title=Pour slab, level 2".
    const previous = fields[fields.length - 1];

    if (!previous) {
      return null;
    }

    previous.value = `${previous.value}, ${part.trim()}`;
  }

  return { typeName: match[1], fields };
}

function parseJsonObject(text: string): AuditField[] | null {
  if (!text.startsWith("{")) {
    return null;
  }

  let parsed: unknown;

  try {
    parsed = JSON.parse(text);
  } catch {
    return null;
  }

  if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
    return null;
  }

  return Object.entries(parsed as Record<string, unknown>).map(([key, value]) =>
    field(
      key,
      typeof value === "string" ? value : value === null ? "null" : JSON.stringify(value),
    ),
  );
}

// Turns the stored text into labelled fields when it has a shape we know,
// and otherwise leaves it as plain text.
export function parseAuditValues(raw: string | null | undefined): AuditValues {
  const text = (raw ?? "").trim();

  if (!text) {
    return { kind: "none" };
  }

  const json = parseJsonObject(text);

  if (json) {
    return { kind: "fields", typeName: null, fields: json };
  }

  const record = parseRecord(text);

  if (record) {
    return { kind: "fields", typeName: record.typeName, fields: record.fields };
  }

  return { kind: "text", text };
}

const SUBJECT_KEYS = ["title", "name", "subject", "summary", "description", "number", "reference"];

function clip(value: string, max: number): string {
  return value.length > max ? `${value.slice(0, max - 1).trimEnd()}…` : value;
}

// A few words saying what the entry was about, taken from its recorded
// values (a title, a name or a number), or null.
export function entrySubject(entry: Pick<AuditEntry, "newValues">): string | null {
  const values = parseAuditValues(entry.newValues);

  if (values.kind !== "fields") {
    return null;
  }

  const usable = (item: AuditField | undefined): item is AuditField =>
    item !== undefined && item.value !== "—";

  for (const wanted of SUBJECT_KEYS) {
    const found = values.fields.find((item) => item.key.toLowerCase() === wanted);

    if (usable(found)) {
      return clip(found.value, 80);
    }
  }

  const numbered = values.fields.find((item) => /number$/i.test(item.key));

  return usable(numbered) ? clip(numbered.value, 80) : null;
}

// ---------------------------------------------------------------------------
// Active filters and paging text
// ---------------------------------------------------------------------------

export interface FilterChip {
  key: keyof AuditFilters;
  label: string;
}

// One removable chip per filter that is switched on.
export function activeFilterChips(
  filters: AuditFilters,
  facets: AuditFacets | null,
): FilterChip[] {
  const chips: FilterChip[] = [];

  const text = filters.q.trim();

  if (text) {
    chips.push({ key: "q", label: `Search: “${text}”` });
  }

  if (filters.projectId) {
    const project = facets?.projects.find((item) => item.projectId === filters.projectId);

    chips.push({
      key: "projectId",
      label: `Project: ${project ? projectOptionLabel(project) : "selected project"}`,
    });
  }

  if (filters.actorUserId) {
    const actor = facets?.actors.find((item) => item.userId === filters.actorUserId);

    chips.push({
      key: "actorUserId",
      label: `Person: ${actor ? actor.name?.trim() || actor.email : "selected person"}`,
    });
  }

  if (filters.entityType) {
    chips.push({ key: "entityType", label: `Kind: ${humanizeEntityType(filters.entityType)}` });
  }

  if (filters.action) {
    chips.push({ key: "action", label: `Action: ${humanizeAction(filters.action)}` });
  }

  if (filters.from) {
    chips.push({ key: "from", label: `From: ${formatDay(filters.from)}` });
  }

  if (filters.to) {
    chips.push({ key: "to", label: `Until: ${formatDay(filters.to)}` });
  }

  return chips;
}

function grouped(value: number): string {
  return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// "Showing 26–50 of 1,234"
export function rangeLabel(
  page: Pick<AuditPage, "page" | "size" | "totalElements" | "items">,
): string {
  if (page.totalElements === 0) {
    return "No entries";
  }

  if (page.items.length === 0) {
    return `No entries on this page (${grouped(page.totalElements)} in total)`;
  }

  const start = page.page * page.size + 1;
  const end = start + page.items.length - 1;

  return `Showing ${grouped(start)}–${grouped(end)} of ${grouped(page.totalElements)}`;
}
