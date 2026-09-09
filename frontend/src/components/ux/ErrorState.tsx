"use client";

export function ErrorState({
  title = "Something went wrong",
  message,
  onRetry,
  compact = false,
}: {
  title?: string;
  message: string;
  onRetry?: () => void;
  compact?: boolean;
}) {
  return (
    <section
      className={
        compact
          ? "uxErrorState compact"
          : "uxErrorState"
      }
      role="alert"
      aria-live="assertive"
    >
      <div
        className="uxErrorStateIcon"
        aria-hidden="true"
      >
        !
      </div>

      <div className="uxErrorStateCopy">
        <strong>
          {title}
        </strong>

        <p>
          {message}
        </p>
      </div>

      {onRetry && (
        <button
          type="button"
          className="uxSecondaryButton"
          onClick={onRetry}
        >
          Try again
        </button>
      )}
    </section>
  );
}
