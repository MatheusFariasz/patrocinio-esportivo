import {
  Link,
  Navigate,
  NavLink,
  Outlet,
  Route,
  Routes,
} from "react-router-dom";
import { useSession } from "./session";
import { Mark } from "./components";
import Entry from "./pages/Entry";
import Dashboard from "./pages/Dashboard";
import Proposal from "./pages/Proposal";
import Contract from "./pages/Contract";

function Protected() {
  const { token } = useSession();
  return token ? <Outlet /> : <Navigate to="/entrar" replace />;
}
function Layout() {
  const { email, logout } = useSession();
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link className="brand" to="/">
          <Mark />
          <span>
            arena<span className="brand-caption">PATROCÍNIOS ESPORTIVOS</span>
          </span>
        </Link>
        <div className="nav-caption">SEU ESPAÇO</div>
        <nav aria-label="Navegação principal">
          <NavLink end to="/">
            <span aria-hidden="true">▦</span> Visão geral
          </NavLink>
          <NavLink to="/propostas/nova">
            <span aria-hidden="true">＋</span> Nova proposta
          </NavLink>
        </nav>
        <div className="sidebar-note">
          <span className="pulse-dot" /> Jogo em equipe.
          <p>Parcerias que levam o esporte mais longe.</p>
        </div>
        <div className="account">
          <span className="avatar">{email.charAt(0).toUpperCase()}</span>
          <div>
            <strong>Sua conta</strong>
            <span title={email}>{email}</span>
          </div>
          <button aria-label="Sair da conta" title="Sair" onClick={logout}>
            ↪
          </button>
        </div>
      </aside>
      <main className="workspace">
        <header className="topbar">
          <span>
            Gestão de parcerias <span className="topbar-slash">/</span>{" "}
            <strong>Área de trabalho</strong>
          </span>
          <span className="topbar-label">
            <span className="pulse-dot" /> Conectado
          </span>
        </header>
        <div className="page">
          <Outlet />
        </div>
        <footer className="app-footer">
          <span>ARENA · PATROCÍNIOS ESPORTIVOS</span>
          <span>IFSP São Carlos</span>
        </footer>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/entrar" element={<Entry />} />
      <Route path="/cadastro" element={<Entry register />} />
      <Route element={<Protected />}>
        <Route element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="propostas/nova" element={<Proposal />} />
          <Route path="propostas/:id/editar" element={<Proposal edit />} />
          <Route path="contratos/:id" element={<Contract />} />
          <Route
            path="*"
            element={
              <div className="empty-state">
                <h1>Página não encontrada</h1>
                <Link className="button primary" to="/">
                  Voltar ao início
                </Link>
              </div>
            }
          />
        </Route>
      </Route>
    </Routes>
  );
}
