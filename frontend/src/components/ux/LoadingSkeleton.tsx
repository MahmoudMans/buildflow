export function LoadingSkeleton({
  lines = 4,
  cards = 0,
  label = "Loading content",
}: {
  lines?: number;
  cards?: number;
  label?: string;
}) {
  return (
    <section
      className="uxSkeleton"
      role="status"
      aria-live="polite"
      aria-label={label}
    >
      <span className="uxSrOnly">
        {label}
      </span>

      {cards > 0 && (
        <div className="uxSkeletonCards">
          {Array.from(
            {
              length: cards,
            },
            (_, index) => (
              <div
                className="uxSkeletonCard"
                key={index}
                aria-hidden="true"
              >
                <span />
                <strong />
              </div>
            )
          )}
        </div>
      )}

      <div
        className="uxSkeletonLines"
        aria-hidden="true"
      >
        {Array.from(
          {
            length: lines,
          },
          (_, index) => (
            <span
              key={index}
              style={{
                width:
                  index ===
                  lines - 1
                    ? "62%"
                    : "100%",
              }}
            />
          )
        )}
      </div>
    </section>
  );
}
