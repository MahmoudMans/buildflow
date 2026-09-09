import {
  humanizeStatus,
  statusTone,
} from "@/lib/ux-validation";

export function StatusBadge({
  value,
  label,
}: {
  value: string;
  label?: string;
}) {
  const tone =
    statusTone(value);

  return (
    <span
      className={`uxStatusBadge ${tone}`}
      data-status={value}
      aria-label={
        label
          ? `${label}: ${humanizeStatus(
              value
            )}`
          : humanizeStatus(value)
      }
    >
      <span
        className="uxStatusDot"
        aria-hidden="true"
      />

      {humanizeStatus(value)}
    </span>
  );
}
