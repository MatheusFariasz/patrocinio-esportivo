import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useSession } from "../session";
import { recentContracts } from "../recent";
import { Alert, Arrow } from "../components";

export default function Dashboard() {
  const { email } = useSession();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const recent = recentContracts(email);
  function search(event) {
    event.preventDefault();
    const id = new FormData(event.currentTarget).get("id").trim();
    if (!/^[1-9]\d*$/.test(id))
      return setError("Informe um número de contrato válido.");
    navigate(`/contratos/${id}`);
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">VISÃO GERAL</span>
          <h1>
            Parcerias em movimento<span className="green-dot">.</span>
          </h1>
          <p className="muted">
            Do primeiro acordo à próxima renovação, acompanhe cada etapa.
          </p>
        </div>
        <Link className="button primary" to="/propostas/nova">
          Nova proposta <span aria-hidden="true">＋</span>
        </Link>
      </div>
      <section className="welcome-banner">
        <div>
          <span className="eyebrow">O PRÓXIMO PASSO COMEÇA AQUI</span>
          <h2>
            Seu time joga.
            <br />
            Sua gestão acompanha.
          </h2>
          <p>
            Encontre um contrato para ver os detalhes ou dê início a uma nova
            parceria.
          </p>
        </div>
        <div className="banner-art" aria-hidden="true">
          <span>↗</span>
          <i />
          <i />
          <i />
        </div>
      </section>
      <div className="dashboard-grid">
        <section className="card search-card">
          <span className="card-icon" aria-hidden="true">
            ⌕
          </span>
          <h2>Consultar contrato</h2>
          <p className="muted">
            Informe o número da proposta ou do contrato para acompanhar sua
            situação.
          </p>
          <Alert>{error}</Alert>
          <form onSubmit={search}>
            <label className="field">
              <span>Número do contrato</span>
              <div className="search-input">
                <span aria-hidden="true">#</span>
                <input
                  name="id"
                  inputMode="numeric"
                  pattern="[1-9][0-9]*"
                  placeholder="Ex.: 12"
                  required
                  aria-label="Número do contrato"
                />
              </div>
            </label>
            <button className="button dark wide">
              Consultar contrato <Arrow />
            </button>
          </form>
        </section>
        <section className="card">
          <div className="section-title">
            <h2>Acessados recentemente</h2>
            <span className="small-label">NESTE NAVEGADOR</span>
          </div>
          {recent.length ? (
            <div className="recent-list">
              {recent.map((id) => (
                <Link key={id} to={`/contratos/${id}`}>
                  <span className="recent-mark">#</span>
                  <span>
                    <strong>Contrato #{id}</strong>
                    <small>Abrir detalhes da parceria</small>
                  </span>
                  <Arrow />
                </Link>
              ))}
            </div>
          ) : (
            <div className="empty-recent">
              <span aria-hidden="true">↗</span>
              <h3>Seu próximo acordo espera por você.</h3>
              <p>Os contratos que você consultar aparecerão aqui.</p>
              <Link to="/propostas/nova">Criar a primeira proposta →</Link>
            </div>
          )}
        </section>
      </div>
      <div className="tip">
        <span aria-hidden="true">◇</span>
        <p>
          <strong>Do acordo ao resultado.</strong> No detalhe do contrato,
          acompanhe parcelas, registre exposição e consulte os períodos
          anteriores.
        </p>
      </div>
    </>
  );
}
