export type StatusTone =
  | "success"
  | "warning"
  | "danger"
  | "info"
  | "neutral";

const canonicalUuid =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const successStatuses =
  new Set([
    "ACTIVE",
    "APPROVED",
    "ACCEPTED",
    "PAID",
    "COMPLETED",
    "DONE",
    "RESOLVED",
    "CLOSED",
    "ON_TRACK",
    "RECEIVED",
  ]);

const warningStatuses =
  new Set([
    "PENDING",
    "PLANNING",
    "ON_HOLD",
    "AT_RISK",
    "PARTIALLY_PAID",
    "PARTIALLY_RECEIVED",
    "SUBMITTED",
    "REVIEW",
    "DRAFT",
  ]);

const dangerStatuses =
  new Set([
    "CRITICAL",
    "CANCELLED",
    "CANCELED",
    "REJECTED",
    "VOID",
    "OVERDUE",
    "BLOCKED",
    "FAILED",
  ]);

const infoStatuses =
  new Set([
    "IN_PROGRESS",
    "ORDERED",
    "ISSUED",
    "SENT",
    "OPEN",
  ]);

export function isCanonicalUuid(
  value: string
) {
  return canonicalUuid.test(
    value.trim()
  );
}

export function humanizeStatus(
  value: string
) {
  return value
    .trim()
    .replace(
      /[_-]+/g,
      " "
    )
    .replace(
      /\s+/g,
      " "
    )
    .toLowerCase()
    .replace(
      /\b\w/g,
      (character) =>
        character.toUpperCase()
    );
}

export function statusTone(
  value: string
): StatusTone {
  const normalized =
    value
      .trim()
      .toUpperCase();

  if (
    successStatuses.has(
      normalized
    )
  ) {
    return "success";
  }

  if (
    warningStatuses.has(
      normalized
    )
  ) {
    return "warning";
  }

  if (
    dangerStatuses.has(
      normalized
    )
  ) {
    return "danger";
  }

  if (
    infoStatuses.has(
      normalized
    )
  ) {
    return "info";
  }

  return "neutral";
}
