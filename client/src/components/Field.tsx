import {
  cloneElement,
  isValidElement,
  type HTMLAttributes,
  type ReactNode,
} from "react";
import type { FieldError } from "react-hook-form";

export function Field({
  id,
  label,
  error,
  hint,
  required = false,
  children,
}: {
  id: string;
  label: string;
  error?: FieldError;
  hint?: string;
  required?: boolean;
  children: ReactNode;
}) {
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;
  const describedBy =
    [hint && hintId, error && errorId].filter(Boolean).join(" ") || undefined;
  const accessibleChildren = isValidElement<HTMLAttributes<HTMLElement>>(
    children,
  )
    ? cloneElement(children, {
        "aria-invalid": Boolean(error),
        "aria-describedby": describedBy,
      })
    : children;
  return (
    <div className={`form-field${error ? " has-error" : ""}`}>
      <label htmlFor={id}>
        {label}
        {required && (
          <span className="required-mark" aria-hidden="true">
            {" "}
            *
          </span>
        )}
      </label>
      {accessibleChildren}
      {hint && (
        <span className="field-hint" id={hintId}>
          {hint}
        </span>
      )}
      {error && (
        <span className="field-error" id={errorId} role="alert">
          {error.message}
        </span>
      )}
    </div>
  );
}
