import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { useSession } from "../session";
import { rememberContract } from "../recent";
import {
  Alert,
  date,
  Field,
  Loading,
  Modal,
  money,
  number,
  Status,
} from "../components";

function Installments({ items, onPay }) {
  if (!items?.length)
    return (
      <div className="inline-empty">
        As parcelas serão geradas quando a proposta for aprovada.
      </div>
    );
  return (
    <div className="table-scroll">
      <table>
        <caption className="sr-only">Parcelas do período</caption>
        <thead>
          <tr>
            <th>Parcela</th>
            <th>Vencimento</th>
            <th>Valor</th>
            <th>Situação</th>
            <th>Pagamento</th>
            {onPay && (
              <th>
                <span className="sr-only">Ações</span>
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.numero}>
              <td>
                <strong>#{item.numero}</strong>
              </td>
              <td>{date(item.vencimento)}</td>
              <td className="numeric">{money(item.valor)}</td>
              <td>
                <span
                  className={`payment-state ${item.paga ? "paid" : item.emAtraso ? "late" : ""}`}
                >
                  {item.paga
                    ? "Paga"
                    : item.emAtraso
                      ? "Em atraso"
                      : "Pendente"}
                </span>
              </td>
              <td>{date(item.dataPagamento)}</td>
              {onPay && (
                <td className="table-action">
                  {!item.paga && (
                    <button className="text-button" onClick={() => onPay(item)}>
                      Registrar pagamento ↗
                    </button>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const operations = {
  aprovar: {
    title: "Aprovar proposta",
    description: "O contrato será ativado e suas parcelas serão geradas.",
    path: (id) => `/api/v1/propostas/${id}/aprovacao`,
    success: "Proposta aprovada. O contrato está ativo.",
  },
  recusar: {
    title: "Recusar proposta",
    description:
      "A proposta será marcada como recusada e não poderá ser editada.",
    path: (id) => `/api/v1/propostas/${id}/recusa`,
    success: "Proposta recusada.",
    danger: true,
  },
  cancelar: {
    title: "Cancelar proposta",
    description: "A proposta será cancelada e não poderá ser editada.",
    path: (id) => `/api/v1/propostas/${id}/cancelamento`,
    success: "Proposta cancelada.",
    danger: true,
  },
  encerrar: {
    title: "Encerrar contrato",
    description:
      "O encerramento gera multa de 20% sobre o valor das parcelas pendentes. Confirme para encerrar a parceria.",
    path: (id) => `/api/v1/contratos/${id}/encerramento`,
    success: "Contrato encerrado. Confira a multa no resumo.",
    danger: true,
  },
  exposicao: {
    title: "Registrar exposição",
    description:
      "Informe o valor de exposição obtido nesta ação. Ele será somado ao total do período.",
    path: (id) => `/api/v1/contratos/${id}/exposicoes`,
    success: "Exposição registrada.",
  },
  renovar: {
    title: "Renovar contrato",
    description:
      "O período atual deve estar encerrado e todas as parcelas precisam estar pagas. A meta atingida permite iniciar o próximo período.",
    path: (id) => `/api/v1/contratos/${id}/renovacao`,
    success: "Contrato renovado. O período anterior foi guardado no histórico.",
  },
  pagamento: {
    title: "Registrar pagamento",
    path: (id, item) =>
      `/api/v1/contratos/${id}/parcelas/${item.numero}/pagamentos`,
    success: "Pagamento registrado.",
  },
};

export default function Contract() {
  const { id } = useParams();
  const location = useLocation();
  const { api, email } = useSession();
  const [contract, setContract] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(location.state?.message || "");
  const [retry, setRetry] = useState(0);
  const [modal, setModal] = useState(null);
  const [modalError, setModalError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setNotice(location.state?.message || "");
    setModal(null);
  }, [id, location.key]);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setContract(null);
    api(`/api/v1/contratos/${encodeURIComponent(id)}`, {
      signal: controller.signal,
    })
      .then((data) => {
        setContract(data);
        rememberContract(email, data.id);
      })
      .catch((failure) => {
        if (failure.name !== "AbortError") setError(failure.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id, retry, api, email]);

  function open(type, item) {
    setModalError("");
    setModal({ type, item });
  }
  async function execute(event) {
    event.preventDefault();
    if (busy) return;
    const values = Object.fromEntries(new FormData(event.currentTarget));
    if (modal.type === "exposicao" && Number(values.valor) <= 0)
      return setModalError("O valor da exposição deve ser maior que zero.");
    if (modal.type === "renovar" && Number(values.novaMeta) <= 0)
      return setModalError("A nova meta deve ser maior que zero.");
    const operation = operations[modal.type];
    let body;
    if (modal.type === "exposicao") body = { valor: values.valor };
    if (modal.type === "renovar")
      body = {
        duracaoMeses: Number(values.duracao),
        novaMeta: values.novaMeta,
      };
    setBusy(true);
    setModalError("");
    try {
      const result = await api(
        operation.path(encodeURIComponent(id), modal.item),
        { method: "POST", body },
      );
      setContract(result);
      setNotice(
        modal.type === "renovar" && result.inicio === contract.inicio
          ? "A meta não foi atingida. O contrato está em risco e o período não foi renovado."
          : operation.success,
      );
      setModal(null);
    } catch (failure) {
      setModalError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <Loading />;
  if (error || !contract)
    return (
      <>
        <Link className="back-link" to="/">
          ← Voltar à consulta
        </Link>
        <div className="card error-card">
          <h1>Não foi possível abrir o contrato.</h1>
          <Alert>{error || "Contrato não encontrado."}</Alert>
          <button
            className="button secondary"
            onClick={() => setRetry((value) => value + 1)}
          >
            Tentar novamente
          </button>
        </div>
      </>
    );
  const pending = contract.status === "PENDENTE";
  const active = ["ATIVO", "EM_RISCO"].includes(contract.status);
  const percentage = Math.min(
    100,
    Math.max(
      0,
      (Number(contract.exposicaoAcumulada) / Number(contract.meta)) * 100,
    ),
  );

  return (
    <>
      <Link className="back-link" to="/">
        ← Visão geral
      </Link>
      <div className="page-heading">
        <div>
          <span className="eyebrow">DETALHES DA PARCERIA</span>
          <div className="title-with-status">
            <h1>
              Contrato <span className="muted">#{contract.id}</span>
            </h1>
            <Status value={contract.status} />
          </div>
          <p className="muted">
            Clube #{contract.clubeId} <span className="dot-separator">·</span>{" "}
            Patrocinador #{contract.patrocinadorId}
          </p>
        </div>
        <button
          className="button secondary"
          onClick={() => {
            setNotice("");
            setRetry((value) => value + 1);
          }}
        >
          ↻ Atualizar
        </button>
      </div>
      <Alert kind="success">{notice}</Alert>
      <div className="metrics">
        <section className="metric">
          <span>INVESTIMENTO TOTAL</span>
          <strong>{money(contract.valorTotal)}</strong>
          <small>Valor do patrocínio no período</small>
        </section>
        <section className="metric">
          <span>VIGÊNCIA</span>
          <strong className="metric-dates">
            {date(contract.inicio)} <span>→</span> {date(contract.termino)}
          </strong>
          <small>Período contratual atual</small>
        </section>
        <section className="metric">
          <span>MULTA RESCISÓRIA</span>
          <strong>{money(contract.multaRescisoria)}</strong>
          <small>Valor apurado no encerramento</small>
        </section>
      </div>
      <section className="card performance">
        <div>
          <span className="eyebrow">EXPOSIÇÃO NO PERÍODO</span>
          <h2>Resultados que fazem a diferença.</h2>
          <p className="muted">
            Acompanhe a exposição acumulada em relação à meta contratual.
          </p>
        </div>
        <div className="progress-summary">
          <div>
            <strong>
              {number(contract.exposicaoAcumulada)}{" "}
              <span>/ {number(contract.meta)}</span>
            </strong>
            <span>{number(percentage)}%</span>
          </div>
          <progress
            value={percentage || 0}
            max="100"
            aria-label="Percentual da meta de exposição atingido"
          />
          <small>
            {Number(contract.exposicaoAcumulada) >= Number(contract.meta)
              ? "Meta atingida neste período."
              : "Cada ação aproxima a parceria da meta."}
          </small>
        </div>
      </section>
      {(pending || active) && (
        <section className="action-bar" aria-label="Operações do contrato">
          <div className="action-buttons">
            {pending ? (
              <>
                <Link
                  className="button secondary small"
                  to={`/propostas/${contract.id}/editar`}
                >
                  Editar proposta
                </Link>
                <button
                  className="button primary small"
                  onClick={() => open("aprovar")}
                >
                  Aprovar
                </button>
                <button
                  className="button secondary small"
                  onClick={() => open("recusar")}
                >
                  Recusar
                </button>
                <button
                  className="text-button danger-text"
                  onClick={() => open("cancelar")}
                >
                  Cancelar proposta
                </button>
              </>
            ) : (
              <>
                <button
                  className="button primary small"
                  onClick={() => open("exposicao")}
                >
                  ＋ Registrar exposição
                </button>
                <button
                  className="button secondary small"
                  onClick={() => open("renovar")}
                >
                  Renovar contrato
                </button>
                <button
                  className="text-button danger-text"
                  onClick={() => open("encerrar")}
                >
                  Encerrar contrato
                </button>
              </>
            )}
          </div>
          {pending && (
            <small>
              Aprovação e recusa exigem perfil de diretor financeiro.
            </small>
          )}
        </section>
      )}
      <section className="card table-card">
        <div className="section-title">
          <div>
            <span className="eyebrow">FINANCEIRO</span>
            <h2>Parcelas do contrato</h2>
          </div>
          <span className="count-badge">
            {contract.parcelas.length}{" "}
            {contract.parcelas.length === 1 ? "parcela" : "parcelas"}
          </span>
        </div>
        <Installments
          items={contract.parcelas}
          onPay={active ? (item) => open("pagamento", item) : null}
        />
      </section>
      <section className="card history">
        <div className="section-title">
          <div>
            <span className="eyebrow">TRAJETÓRIA DA PARCERIA</span>
            <h2>Períodos anteriores</h2>
          </div>
          <span className="count-badge">{contract.historico.length}</span>
        </div>
        {contract.historico.length ? (
          contract.historico.map((item, index) => (
            <details key={`${item.inicio}-${index}`}>
              <summary>
                <span>
                  <strong>Período {index + 1}</strong>
                  <small>
                    {date(item.inicio)} — {date(item.termino)}
                  </small>
                </span>
                <span>
                  Ver detalhes <span aria-hidden="true">＋</span>
                </span>
              </summary>
              <div className="history-summary">
                Meta: <strong>{number(item.meta)}</strong>
                <span>
                  Exposição: <strong>{number(item.exposicaoAcumulada)}</strong>
                </span>
              </div>
              <Installments items={item.parcelas} />
            </details>
          ))
        ) : (
          <p className="inline-empty">
            O histórico aparece aqui após a primeira renovação.
          </p>
        )}
      </section>
      {modal && (
        <Modal
          title={operations[modal.type].title}
          onClose={() => setModal(null)}
          busy={busy}
        >
          <p className="muted">
            {modal.type === "pagamento"
              ? `Confirmar o pagamento da parcela #${modal.item.numero}, no valor de ${money(modal.item.valor)}? A data de hoje será registrada.`
              : operations[modal.type].description}
          </p>
          <Alert>{modalError}</Alert>
          <form className="stack" onSubmit={execute}>
            {modal.type === "exposicao" && (
              <Field label="Valor da exposição">
                <input
                  name="valor"
                  type="number"
                  min="0"
                  step="any"
                  required
                  autoFocus
                />
              </Field>
            )}
            {modal.type === "renovar" && (
              <>
                <Field label="Duração do novo período (meses)">
                  <input
                    name="duracao"
                    type="number"
                    min="1"
                    step="1"
                    required
                    autoFocus
                  />
                </Field>
                <Field label="Nova meta contratual">
                  <input
                    name="novaMeta"
                    type="number"
                    min="0"
                    step="any"
                    required
                  />
                </Field>
              </>
            )}
            <div className="modal-footer">
              <button
                type="button"
                className="button secondary"
                disabled={busy}
                onClick={() => setModal(null)}
              >
                Voltar
              </button>
              <button
                className={`button ${operations[modal.type].danger ? "danger" : "primary"}`}
                disabled={busy}
              >
                {busy ? "Salvando…" : "Confirmar"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
