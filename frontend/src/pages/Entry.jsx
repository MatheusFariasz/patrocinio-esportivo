import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useSession } from "../session";
import { Alert, Arrow, Field, Mark } from "../components";

export default function Entry({ register = false }) {
  const { token, api, login, notice } = useSession();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  if (token) return <Navigate to="/" replace />;

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const values = Object.fromEntries(new FormData(event.currentTarget));
    setBusy(true);
    setError("");
    setSuccess("");
    let created = false;
    try {
      if (register) {
        if (!values.name.trim() || !values.lastname.trim())
          throw new Error("Preencha seu nome e sobrenome.");
        if (values.password !== values.confirm)
          throw new Error("As senhas precisam ser iguais.");
        await api("/api/v1/register", {
          method: "POST",
          authenticated: false,
          body: {
            name: values.name.trim(),
            lastname: values.lastname.trim(),
            email: values.email.trim(),
            password: values.password,
          },
        });
        setSuccess("Conta criada. Entrando no seu espaço…");
        created = true;
      }
      await login(values.email, values.password);
      navigate("/", { replace: true });
    } catch (failure) {
      if (created) setSuccess("Conta criada com sucesso.");
      setError(
        created
          ? `Sua conta foi criada, mas a entrada não foi concluída. Acesse “Entre aqui” para tentar novamente. ${failure.message}`
          : failure.message,
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-shell">
      <section className="auth-main">
        <Link className="brand brand-dark" to="/entrar">
          <Mark />
          <span>
            arena<span className="brand-caption">PATROCÍNIOS ESPORTIVOS</span>
          </span>
        </Link>
        <div className="auth-form-wrap">
          <span className="eyebrow">CONEXÕES QUE MOVEM O ESPORTE</span>
          <h1>{register ? "Faça parte do jogo." : "Bom ter você de volta."}</h1>
          <p className="muted">
            {register
              ? "Crie sua conta para começar a construir novas parcerias."
              : "Entre para acompanhar suas propostas e contratos."}
          </p>
          <Alert>{error || notice}</Alert>
          <Alert kind="success">{success}</Alert>
          <form
            className="stack"
            onSubmit={submit}
            key={register ? "register" : "login"}
          >
            {register && (
              <div className="form-grid">
                <Field label="Nome">
                  <input
                    name="name"
                    autoComplete="given-name"
                    required
                    maxLength="80"
                  />
                </Field>
                <Field label="Sobrenome">
                  <input
                    name="lastname"
                    autoComplete="family-name"
                    required
                    maxLength="120"
                  />
                </Field>
              </div>
            )}
            <Field label="E-mail">
              <input
                type="email"
                name="email"
                placeholder="voce@exemplo.com"
                autoComplete="username"
                required
              />
            </Field>
            <Field label="Senha">
              <input
                type="password"
                name="password"
                autoComplete={register ? "new-password" : "current-password"}
                required
              />
            </Field>
            {register && (
              <Field label="Confirmar senha">
                <input
                  type="password"
                  name="confirm"
                  autoComplete="new-password"
                  required
                />
              </Field>
            )}
            <button className="button primary wide" disabled={busy}>
              {busy
                ? "Aguarde…"
                : register
                  ? "Criar minha conta"
                  : "Entrar na Arena"}
              <Arrow />
            </button>
          </form>
          <p className="auth-switch">
            {register ? "Já tem uma conta?" : "Ainda não tem conta?"}{" "}
            <Link
              to={register ? "/entrar" : "/cadastro"}
              onClick={() => {
                setError("");
                setSuccess("");
              }}
            >
              {register ? "Entre aqui" : "Cadastre-se"}
            </Link>
          </p>
        </div>
        <footer className="auth-footer">
          Um espaço para o esporte crescer. <span>IFSP São Carlos</span>
        </footer>
      </section>
      <aside
        className="auth-art"
        aria-label="Arena, gestão de patrocínios esportivos"
      >
        <div className="art-label">
          <span className="pulse-dot" /> MAIS QUE UM CONTRATO. UMA PARCERIA.
        </div>
        <div className="running-track" aria-hidden="true">
          <i />
          <i />
          <i />
          <i />
          <span>↗</span>
        </div>
        <div className="art-copy">
          <span className="eyebrow">
            DO PRIMEIRO CONTATO À PRÓXIMA CONQUISTA
          </span>
          <h2>
            Boas parcerias.
            <br />
            Grandes <em>conquistas.</em>
          </h2>
          <p>Propostas, pagamentos e resultados. Tudo no mesmo campo.</p>
        </div>
        <div className="art-bottom">
          <span>ARENA / GESTÃO ESPORTIVA</span>
          <span>01 — ∞</span>
        </div>
      </aside>
    </div>
  );
}
