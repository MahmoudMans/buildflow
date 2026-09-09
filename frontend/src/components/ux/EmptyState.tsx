import type {
  ReactNode,
} from "react";

export function EmptyState({
  title,
  description,
  action,
  compact = false,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  compact?: boolean;
}) {
  return (
    <section
      className={
        compact
          ? "uxEmptyState compact"
          : "uxEmptyState"
      }
      aria-label={title}
    >
      <div
        className="uxEmptyStateIcon"
        aria-hidden="true"
      >
        —
      </div>

      <div>
        <strong>
          {title}
        </strong>

        {description && (
          <p>
            {description}
          </p>
        )}
      </div>

      {action && (
        <div className="uxEmptyStateAction">
          {action}
        </div>
      )}
    </section>
  );
}
