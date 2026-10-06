import {
  formatReportDay,
  parseWorkers,
  photoSummary,
  stepForField,
} from "@/lib/site-capture";
import type {
  CaptureDraft,
  CaptureErrors,
  CaptureField,
  CapturePhoto,
  CaptureStep,
} from "@/lib/site-capture";

type Row = {
  label: string;
  field: CaptureField;
  value: string;
  // Optional rows are left out when nothing was written.
  optional: boolean;
};

function rows(draft: CaptureDraft): Row[] {
  const workers = parseWorkers(draft.workersOnSite);

  return [
    { label: "Date", field: "reportDate", value: formatReportDay(draft.reportDate), optional: false },
    { label: "Weather", field: "weatherText", value: draft.weatherText.trim(), optional: false },
    {
      label: "Workers on site",
      field: "workersOnSite",
      value: workers === null ? draft.workersOnSite : String(workers),
      optional: false,
    },
    { label: "Summary", field: "summary", value: draft.summary.trim(), optional: false },
    { label: "Work completed", field: "workCompleted", value: draft.workCompleted.trim(), optional: true },
    { label: "Delays", field: "delays", value: draft.delays.trim(), optional: true },
    { label: "Issues or problems", field: "issues", value: draft.issues.trim(), optional: true },
    { label: "Next steps", field: "nextSteps", value: draft.nextSteps.trim(), optional: true },
  ];
}

// Step 4: everything in one place before it is sent.
export function SiteCaptureReview({
  draft,
  photos,
  errors,
  onEdit,
}: {
  draft: CaptureDraft;
  photos: CapturePhoto[];
  errors: CaptureErrors;
  onEdit: (step: CaptureStep) => void;
}) {
  const problems = (Object.keys(errors) as CaptureField[]).filter((field) => errors[field]);

  return (
    <div className="uxCaptureReview">
      {problems.length > 0 ? (
        <div className="uxCaptureProblems" role="alert">
          <strong>Fix this before sending:</strong>

          <ul>
            {problems.map((field) => (
              <li key={field}>
                <button
                  type="button"
                  className="uxLinkButton"
                  onClick={() => onEdit(stepForField(field))}
                >
                  {errors[field]}
                </button>
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      <dl className="uxCaptureSummary">
        {rows(draft)
          .filter((row) => !row.optional || row.value !== "")
          .map((row) => (
            <div key={row.field} className="uxCaptureSummaryRow">
              <dt>{row.label}</dt>

              <dd className={row.value === "" ? "uxCaptureNone" : undefined}>
                {row.value === "" ? "Not recorded" : row.value}
              </dd>

              <dd>
                <button
                  type="button"
                  className="uxLinkButton"
                  aria-label={`Change ${row.label.toLowerCase()}`}
                  onClick={() => onEdit(stepForField(row.field))}
                >
                  Change
                </button>
              </dd>
            </div>
          ))}

        <div className="uxCaptureSummaryRow">
          <dt>Photos</dt>

          <dd className={photos.length === 0 ? "uxCaptureNone" : undefined}>
            {photoSummary(photos)}
          </dd>

          <dd>
            <button
              type="button"
              className="uxLinkButton"
              aria-label="Change photos"
              onClick={() => onEdit("photos")}
            >
              Change
            </button>
          </dd>
        </div>
      </dl>

      {photos.length > 0 ? (
        <ul className="uxCaptureThumbs" aria-label="Photos to send">
          {photos.slice(0, 8).map((photo, index) => (
            <li key={photo.id}>
              <div
                className="uxCaptureThumb"
                role="img"
                aria-label={`Photo ${index + 1}`}
                style={{ backgroundImage: `url("${photo.dataUrl}")` }}
              />
            </li>
          ))}

          {photos.length > 8 ? <li className="uxCaptureMore">+{photos.length - 8}</li> : null}
        </ul>
      ) : null}
    </div>
  );
}
