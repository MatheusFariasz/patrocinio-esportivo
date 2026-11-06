import { useEffect, useRef } from "react";

export function Mark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <svg viewBox="0 0 40 40">
        <path d="M7 31 17 9h6l10 22h-8l-5-13-6 13Z" fill="currentColor" />
        <path d="M17 27h9" stroke="currentColor" strokeWidth="4" />
      </svg>
    </span>
  );
}
export function Arrow() {
  return <span aria-hidden="true">↗</span>;
}
export function Field({ label, hint, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
export function Alert({ children, kind = "error" }) {
  return children ? (
    <div
      className={`alert ${kind}`}
      role={kind === "error" ? "alert" : "status"}
    >
      {children}
    </div>
  ) : null;
}
export function Loading() {
  return (
    <div className="loading" role="status">
      <span className="spinner" />
      Carregando…
    </div>
  );
}
export const statusNames = {
  PENDENTE: "Pendente",
  ATIVO: "Ativo",
  EM_RISCO: "Em risco",
  ENCERRADO: "Encerrado",
  RECUSADO: "Recusado",
  CANCELADO: "Cancelado",
};
export function Status({ value }) {
  return (
    <span className={`status status-${value?.toLowerCase()}`}>
      {statusNames[value] || value}
    </span>
  );
}
export const money = (value) =>
  Number(value || 0).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
export const number = (value) =>
  Number(value || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 });
export const date = (value) =>
  value
    ? new Intl.DateTimeFormat("pt-BR").format(new Date(`${value}T12:00:00`))
    : "—";

export function Modal({ title, children, onClose, busy }) {
  const ref = useRef(null);
  useEffect(() => {
    const dialog = ref.current;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return (
    <dialog
      ref={ref}
      className="modal"
      aria-labelledby="modal-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
    >
      <div className="modal-heading">
        <h2 id="modal-title">{title}</h2>
        <button
          type="button"
          className="icon-button"
          aria-label="Fechar"
          disabled={busy}
          onClick={onClose}
        >
          ×
        </button>
      </div>
      {children}
    </dialog>
  );
}
