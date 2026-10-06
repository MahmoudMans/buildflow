import { CAPTURE_STEPS, stepIndex } from "@/lib/site-capture";
import type { CaptureStep } from "@/lib/site-capture";

// Where the person is in the report. Steps already passed can be tapped to go
// back; the ones ahead are reached with the Next button.
export function SiteCaptureStepper({
  current,
  onGoTo,
}: {
  current: CaptureStep;
  onGoTo: (step: CaptureStep) => void;
}) {
  const currentIndex = stepIndex(current);

  return (
    <nav aria-label="Report steps">
      <ol className="uxCaptureSteps">
        {CAPTURE_STEPS.map((step, index) => {
          const state =
            index < currentIndex ? "done" : index === currentIndex ? "current" : "todo";

          return (
            <li
              key={step.key}
              className={`uxCaptureStep ${state}`}
              aria-current={state === "current" ? "step" : undefined}
            >
              {state === "done" ? (
                <button
                  type="button"
                  className="uxCaptureStepButton"
                  aria-label={`Back to ${step.label}`}
                  onClick={() => onGoTo(step.key)}
                >
                  <span className="uxCaptureStepDot" aria-hidden="true">
                    ✓
                  </span>
                  <span className="uxCaptureStepLabel">{step.label}</span>
                </button>
              ) : (
                <span className="uxCaptureStepButton">
                  <span className="uxCaptureStepDot" aria-hidden="true">
                    {index + 1}
                  </span>
                  <span className="uxCaptureStepLabel">{step.label}</span>
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
