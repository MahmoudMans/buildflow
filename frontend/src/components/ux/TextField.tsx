import type {
  InputHTMLAttributes,
} from "react";

type TextFieldProps =
  Omit<
    InputHTMLAttributes<HTMLInputElement>,
    "id"
  > & {
    id: string;
    label: string;
    hint?: string;
    error?: string;
  };

export function TextField({
  id,
  label,
  hint,
  error,
  required,
  ...input
}: TextFieldProps) {
  const hintId =
    hint
      ? `${id}-hint`
      : undefined;

  const errorId =
    error
      ? `${id}-error`
      : undefined;

  const describedBy =
    [
      hintId,
      errorId,
    ]
      .filter(Boolean)
      .join(" ")
      || undefined;

  return (
    <label
      className="uxFormField"
      htmlFor={id}
    >
      <span>
        {label}

        {required && (
          <span
            className="uxRequiredMarker"
            aria-hidden="true"
          >
            *
          </span>
        )}
      </span>

      <input
        {...input}
        id={id}
        required={required}
        aria-invalid={
          error
            ? true
            : undefined
        }
        aria-describedby={describedBy}
      />

      {hint && (
        <small
          id={hintId}
          className="uxFieldHint"
        >
          {hint}
        </small>
      )}

      {error && (
        <small
          id={errorId}
          className="uxFieldError"
          role="alert"
        >
          {error}
        </small>
      )}
    </label>
  );
}
