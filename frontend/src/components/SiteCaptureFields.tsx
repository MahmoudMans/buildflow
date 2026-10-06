import { useId } from "react";

import {
  CAPTURE_WORKERS_MAX,
  WEATHER_CHOICES,
  parseWorkers,
} from "@/lib/site-capture";
import type { CaptureDraft, CaptureErrors } from "@/lib/site-capture";

type FieldsProps = {
  draft: CaptureDraft;
  errors: CaptureErrors;
  onChange: (patch: Partial<CaptureDraft>) => void;
};

function Problem({ id, message }: { id: string; message: string | undefined }) {
  if (!message) {
    return null;
  }

  return (
    <p id={id} className="uxFieldError">
      {message}
    </p>
  );
}

// Step 1: the date, the weather and how many people were working.
export function BasicsStep({
  draft,
  errors,
  today,
  onChange,
}: FieldsProps & {
  // The latest day that can be chosen: today where the organization is.
  today: string;
}) {
  const id = useId();
  const workers = parseWorkers(draft.workersOnSite) ?? 0;

  return (
    <div className="uxCaptureFields">
      <div className="uxCaptureField">
        <label htmlFor={`${id}-date`}>Report date</label>

        <input
          id={`${id}-date`}
          type="date"
          value={draft.reportDate}
          max={today}
          aria-invalid={errors.reportDate ? true : undefined}
          aria-describedby={errors.reportDate ? `${id}-date-error` : undefined}
          onChange={(event) => onChange({ reportDate: event.target.value })}
        />

        <Problem id={`${id}-date-error`} message={errors.reportDate} />
      </div>

      <div className="uxCaptureField">
        <label htmlFor={`${id}-weather`}>Weather</label>

        <div className="uxCaptureChoices" role="group" aria-label="Quick weather choices">
          {WEATHER_CHOICES.map((choice) => (
            <button
              key={choice}
              type="button"
              className="uxCaptureChoice"
              aria-pressed={draft.weatherText === choice}
              onClick={() =>
                onChange({ weatherText: draft.weatherText === choice ? "" : choice })
              }
            >
              {choice}
            </button>
          ))}
        </div>

        <input
          id={`${id}-weather`}
          type="text"
          value={draft.weatherText}
          autoComplete="off"
          placeholder="Or describe it in your own words"
          aria-invalid={errors.weatherText ? true : undefined}
          aria-describedby={errors.weatherText ? `${id}-weather-error` : undefined}
          onChange={(event) => onChange({ weatherText: event.target.value })}
        />

        <Problem id={`${id}-weather-error`} message={errors.weatherText} />
      </div>

      <div className="uxCaptureField">
        <label htmlFor={`${id}-workers`}>Workers on site</label>

        <div className="uxCaptureCounter">
          <button
            type="button"
            className="uxCaptureCounterButton"
            aria-label="One fewer worker"
            disabled={workers <= 0}
            onClick={() => onChange({ workersOnSite: String(Math.max(0, workers - 1)) })}
          >
            −
          </button>

          <input
            id={`${id}-workers`}
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            value={draft.workersOnSite}
            autoComplete="off"
            placeholder="0"
            aria-invalid={errors.workersOnSite ? true : undefined}
            aria-describedby={errors.workersOnSite ? `${id}-workers-error` : undefined}
            onChange={(event) => onChange({ workersOnSite: event.target.value })}
          />

          <button
            type="button"
            className="uxCaptureCounterButton"
            aria-label="One more worker"
            disabled={workers >= CAPTURE_WORKERS_MAX}
            onClick={() =>
              onChange({ workersOnSite: String(Math.min(CAPTURE_WORKERS_MAX, workers + 1)) })
            }
          >
            +
          </button>
        </div>

        <Problem id={`${id}-workers-error`} message={errors.workersOnSite} />
      </div>
    </div>
  );
}

function TextBox({
  label,
  hint,
  value,
  rows,
  error,
  required,
  onChange,
}: {
  label: string;
  hint?: string;
  value: string;
  rows: number;
  error: string | undefined;
  required?: boolean;
  onChange: (value: string) => void;
}) {
  const id = useId();
  const describedBy = [hint ? `${id}-hint` : "", error ? `${id}-error` : ""]
    .filter(Boolean)
    .join(" ");

  return (
    <div className="uxCaptureField">
      <label htmlFor={id}>
        {label}
        {required ? <span className="uxCaptureRequired"> (required)</span> : null}
      </label>

      {hint ? (
        <p id={`${id}-hint`} className="uxCaptureHint">
          {hint}
        </p>
      ) : null}

      <textarea
        id={id}
        rows={rows}
        value={value}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy || undefined}
        onChange={(event) => onChange(event.target.value)}
      />

      <Problem id={`${id}-error`} message={error} />
    </div>
  );
}

// Step 2: what happened on site.
export function WorkStep({ draft, errors, onChange }: FieldsProps) {
  return (
    <div className="uxCaptureFields">
      <TextBox
        label="Summary"
        required
        hint="A few lines on how the day went."
        rows={4}
        value={draft.summary}
        error={errors.summary}
        onChange={(summary) => onChange({ summary })}
      />

      <TextBox
        label="Work completed"
        rows={3}
        value={draft.workCompleted}
        error={errors.workCompleted}
        onChange={(workCompleted) => onChange({ workCompleted })}
      />

      <TextBox
        label="Delays"
        rows={3}
        value={draft.delays}
        error={errors.delays}
        onChange={(delays) => onChange({ delays })}
      />

      <TextBox
        label="Issues or problems"
        rows={3}
        value={draft.issues}
        error={errors.issues}
        onChange={(issues) => onChange({ issues })}
      />

      <TextBox
        label="Next steps"
        rows={3}
        value={draft.nextSteps}
        error={errors.nextSteps}
        onChange={(nextSteps) => onChange({ nextSteps })}
      />
    </div>
  );
}
