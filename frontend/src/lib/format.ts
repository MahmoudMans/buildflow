const LOCALE = "en-GB";

export function formatMoney(value: number, currency: string): string {
  return new Intl.NumberFormat(LOCALE, {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(value);
}

export function formatCompactMoney(value: number, currency: string): string {
  return new Intl.NumberFormat(LOCALE, {
    style: "currency",
    currency,
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(value);
}

export function formatDate(
  value: string | null | undefined,
  fallback = "—",
): string {
  if (!value) {
    return fallback;
  }

  const date = value.includes("T")
    ? new Date(value)
    : new Date(`${value}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return fallback;
  }

  return new Intl.DateTimeFormat(LOCALE, {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(date);
}
