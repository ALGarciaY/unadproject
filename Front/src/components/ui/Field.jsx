import { useId } from "react";
import Icon from "./Icon";

const controlBase =
  "w-full rounded-lg border bg-surface text-sm text-fg transition-colors placeholder:text-fg-subtle " +
  "focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/25 " +
  "disabled:cursor-not-allowed disabled:bg-subtle disabled:text-fg-muted";

const controlTone = (invalid) => (invalid ? "border-danger" : "border-line");

// Tipos de <input> donde "pattern" aplica segun el estandar HTML. En los
// demas (checkbox, file, date, number...) el navegador simplemente lo ignora.
const PATTERNABLE_TYPES = new Set([undefined, "text", "email", "search", "tel", "url", "password"]);
// Exige al menos un caracter que no sea espacio: "required" por si solo deja
// pasar un valor que es solo espacios en blanco.
const NOT_BLANK_PATTERN = ".*\\S.*";
const NOT_BLANK_TITLE = "Este campo no puede quedar vacío ni tener solo espacios.";

function FieldShell({ id, label, hint, error, required, children, className = "" }) {
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      {label && (
        <label htmlFor={id} className="text-[13px] font-semibold text-fg">
          {label}
          {required && <span className="ml-0.5 text-danger">*</span>}
        </label>
      )}

      {children}

      {error ? (
        <p className="flex items-center gap-1 text-xs font-medium text-danger">
          <Icon name="error" size={14} />
          {error}
        </p>
      ) : (
        hint && <p className="text-xs text-fg-subtle">{hint}</p>
      )}
    </div>
  );
}

export function Input({
  label,
  hint,
  error,
  icon,
  trailing,
  required = false,
  className = "",
  wrapperClassName = "",
  id,
  ...props
}) {
  const generatedId = useId();
  const inputId = id || generatedId;
  const needsNotBlank = required && PATTERNABLE_TYPES.has(props.type);

  const input = (
    <input
      id={inputId}
      required={required}
      pattern={needsNotBlank ? NOT_BLANK_PATTERN : undefined}
      title={needsNotBlank ? NOT_BLANK_TITLE : undefined}
      aria-invalid={error ? true : undefined}
      className={`${controlBase} ${controlTone(Boolean(error))} ${
        icon ? "pl-10" : "px-3.5"
      } ${trailing ? "pr-11" : ""} py-2.5 ${className}`}
      {...props}
    />
  );

  return (
    <FieldShell
      id={inputId}
      label={label}
      hint={hint}
      error={error}
      required={required}
      className={wrapperClassName}
    >
      {icon || trailing ? (
        <div className="relative">
          {icon && (
            <Icon
              name={icon}
              size={18}
              className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-fg-subtle"
            />
          )}
          {input}
          {trailing && (
            <span className="absolute right-1.5 top-1/2 -translate-y-1/2">{trailing}</span>
          )}
        </div>
      ) : (
        input
      )}
    </FieldShell>
  );
}

export function Select({
  label,
  hint,
  error,
  required = false,
  className = "",
  wrapperClassName = "",
  children,
  id,
  ...props
}) {
  const generatedId = useId();
  const selectId = id || generatedId;

  return (
    <FieldShell
      id={selectId}
      label={label}
      hint={hint}
      error={error}
      required={required}
      className={wrapperClassName}
    >
      <div className="relative">
        <select
          id={selectId}
          required={required}
          aria-invalid={error ? true : undefined}
          className={`${controlBase} ${controlTone(Boolean(error))} appearance-none py-2.5 pl-3.5 pr-10 ${className}`}
          {...props}
        >
          {children}
        </select>
        <Icon
          name="expand_more"
          size={18}
          className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-fg-subtle"
        />
      </div>
    </FieldShell>
  );
}

export function Textarea({
  label,
  hint,
  error,
  required = false,
  rows = 3,
  className = "",
  wrapperClassName = "",
  id,
  ...props
}) {
  const generatedId = useId();
  const textareaId = id || generatedId;

  return (
    <FieldShell
      id={textareaId}
      label={label}
      hint={hint}
      error={error}
      required={required}
      className={wrapperClassName}
    >
      <textarea
        id={textareaId}
        rows={rows}
        required={required}
        aria-invalid={error ? true : undefined}
        className={`${controlBase} ${controlTone(Boolean(error))} resize-y px-3.5 py-2.5 leading-6 ${className}`}
        {...props}
      />
    </FieldShell>
  );
}

/**
 * Carga de archivos con una zona clicable y la lista de lo ya seleccionado.
 * Se usa tanto para materiales PDF como para la foto de perfil.
 */
export function FileDrop({
  label,
  hint,
  accept,
  multiple = false,
  files = [],
  onFilesSelected,
  icon = "upload_file",
  disabled = false,
  className = "",
  inputRef,
}) {
  const generatedId = useId();
  const inputId = inputRef ? undefined : `${generatedId}-file`;

  return (
    <FieldShell label={label} hint={hint} className={className}>
      <label
        htmlFor={inputId}
        className={`flex cursor-pointer items-center justify-center gap-2 rounded-lg border border-dashed px-4 py-3 text-sm font-semibold transition-colors ${
          disabled
            ? "cursor-not-allowed border-line bg-subtle text-fg-subtle"
            : "border-brand/40 bg-brand-soft/60 text-brand-ink hover:border-brand hover:bg-brand-soft"
        }`}
      >
        <Icon name={icon} size={18} />
        <span>{multiple ? "Seleccionar archivos" : "Seleccionar archivo"}</span>
        <input
          id={inputId}
          ref={inputRef}
          type="file"
          accept={accept}
          multiple={multiple}
          disabled={disabled}
          className="hidden"
          onChange={(event) => onFilesSelected?.(Array.from(event.target.files || []))}
        />
      </label>

      {files.length > 0 && (
        <ul className="mt-1 flex flex-col gap-1.5">
          {files.map((file) => (
            <li
              key={`${file.name}-${file.size}`}
              className="flex items-center gap-2 rounded-lg bg-subtle px-3 py-2 text-xs font-semibold text-fg-muted"
            >
              <Icon name="description" size={14} className="shrink-0" />
              <span className="truncate">{file.name}</span>
            </li>
          ))}
        </ul>
      )}
    </FieldShell>
  );
}
