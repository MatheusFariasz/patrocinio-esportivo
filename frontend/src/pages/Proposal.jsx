import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useSession } from "../session";
import { rememberContract } from "../recent";
import { Alert, Arrow, Field, Loading } from "../components";

export default function Proposal({ edit = false }) {
  const { id } = useParams();
  const { api, email } = useSession();
  const navigate = useNavigate();
  const [contract, setContract] = useState(null);
  const [loading, setLoading] = useState(edit);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    setError("");
    setContract(null);
    setLoading(edit);
    if (!edit) return;
    const controller = new AbortController();
    api(`/api/v1/contratos/${encodeURIComponent(id)}`, {
      signal: controller.signal,
    })
      .then((data) => setContract(data))
      .catch((failure) => {
        if (failure.name !== "AbortError") setError(failure.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id, edit, api]);

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const form = Object.fromEntries(new FormData(event.currentTarget));
    if (form.termino <= form.inicio)
      return setError(
        "A data de término precisa ser posterior à data de início.",
      );
    if (["valor", "meta"].some((key) => Number(form[key]) <= 0))
      return setError("Valor e meta precisam ser maiores que zero.");
    const body = {
      valor: form.valor,
      inicio: form.inicio,
      termino: form.termino,
      meta: form.meta,
    };
    if (!edit) {
      body.clubeId = form.clubeId;
      body.patrocinadorId = form.patrocinadorId;
    }
    setBusy(true);
    setError("");
    try {
      const data = await api(
        edit
          ? `/api/v1/propostas/${encodeURIComponent(id)}`
          : "/api/v1/propostas",
        { method: edit ? "PUT" : "POST", body },
      );
      rememberContract(email, data.id);
      navigate(`/contratos/${data.id}`, {
        state: {
          message: edit
            ? "Proposta atualizada com sucesso."
            : "Proposta criada. Anote o número para acompanhar a parceria.",
        },
      });
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Link className="back-link" to={edit ? `/contratos/${id}` : "/"}>
        ← {edit ? "Voltar ao contrato" : "Visão geral"}
      </Link>
      <div className="page-heading">
        <div>
          <span className="eyebrow">
            {edit ? `PROPOSTA #${id}` : "UM NOVO COMEÇO"}
          </span>
          <h1>
            {edit ? "Ajuste os detalhes." : "Vamos construir uma parceria."}
          </h1>
          <p className="muted">
            {edit
              ? "Atualize os dados enquanto a proposta está pendente."
              : "Preencha os dados para enviar uma proposta de patrocínio."}
          </p>
        </div>
      </div>
      <Alert>{error}</Alert>
      {loading ? (
        <Loading />
      ) : edit && !contract ? (
        <Link className="button secondary" to="/">
          Voltar à consulta
        </Link>
      ) : edit && contract.status !== "PENDENTE" ? (
        <Alert>Esta proposta já foi avaliada e não pode ser editada.</Alert>
      ) : (
        <div className="proposal-layout">
          <form
            className="card proposal-form"
            onSubmit={submit}
            key={edit ? id : "nova"}
          >
            <div className="form-section-title">
              <span>01</span>
              <div>
                <h2>As partes da parceria</h2>
                <p>Clube e patrocinador vinculados ao acordo.</p>
              </div>
            </div>
            <div className="form-grid">
              <Field
                label="Identificador do clube"
                hint="Utilize um clube já cadastrado."
              >
                <input
                  name="clubeId"
                  inputMode="numeric"
                  pattern="[1-9][0-9]*"
                  required={!edit}
                  defaultValue={contract?.clubeId || ""}
                  disabled={edit}
                  placeholder="Ex.: 10"
                />
              </Field>
              <Field
                label="Identificador do patrocinador"
                hint="Utilize um patrocinador já cadastrado."
              >
                <input
                  name="patrocinadorId"
                  inputMode="numeric"
                  pattern="[1-9][0-9]*"
                  required={!edit}
                  defaultValue={contract?.patrocinadorId || ""}
                  disabled={edit}
                  placeholder="Ex.: 20"
                />
              </Field>
            </div>
            <div className="form-section-title">
              <span>02</span>
              <div>
                <h2>Condições do patrocínio</h2>
                <p>Defina o investimento, a meta e o período.</p>
              </div>
            </div>
            <div className="form-grid">
              <Field label="Valor total (R$)">
                <input
                  name="valor"
                  type="number"
                  min="0.01"
                  step="0.01"
                  required
                  defaultValue={contract?.valorTotal || ""}
                  placeholder="0,00"
                />
              </Field>
              <Field
                label="Meta contratual"
                hint="Valor de exposição esperado para o período."
              >
                <input
                  name="meta"
                  type="number"
                  min="0"
                  step="any"
                  required
                  defaultValue={contract?.meta || ""}
                  placeholder="Ex.: 500"
                />
              </Field>
              <Field label="Data de início">
                <input
                  name="inicio"
                  type="date"
                  required
                  defaultValue={contract?.inicio || ""}
                />
              </Field>
              <Field label="Data de término">
                <input
                  name="termino"
                  type="date"
                  required
                  defaultValue={contract?.termino || ""}
                />
              </Field>
            </div>
            <div className="form-footer">
              <Link
                className="button secondary"
                to={edit ? `/contratos/${id}` : "/"}
              >
                Cancelar
              </Link>
              <button className="button primary" disabled={busy}>
                {busy
                  ? "Salvando…"
                  : edit
                    ? "Salvar alterações"
                    : "Criar proposta"}
                <Arrow />
              </button>
            </div>
          </form>
          <aside className="proposal-note">
            <span className="eyebrow">CADA ETAPA CONTA</span>
            <h2>Uma boa parceria começa com clareza.</h2>
            <ol>
              <li>
                <strong>Proposta pendente</strong>
                <p>Os dados podem ser ajustados antes da avaliação.</p>
              </li>
              <li>
                <strong>Avaliação financeira</strong>
                <p>O diretor financeiro aprova ou recusa o acordo.</p>
              </li>
              <li>
                <strong>Contrato em campo</strong>
                <p>Após a aprovação, acompanhe parcelas e exposição.</p>
              </li>
            </ol>
          </aside>
        </div>
      )}
    </>
  );
}
