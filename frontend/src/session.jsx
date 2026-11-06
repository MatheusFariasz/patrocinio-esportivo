import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { createApi } from "./api";

const Context = createContext(null);
const KEY = "arena.session";

function read() {
  try {
    return sessionStorage.getItem(KEY);
  } catch {
    return null;
  }
}
function persist(token) {
  try {
    token ? sessionStorage.setItem(KEY, token) : sessionStorage.removeItem(KEY);
  } catch {
    /* A sessão em memória continua disponível. */
  }
}
function claims(token) {
  try {
    const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(
      new TextDecoder().decode(
        Uint8Array.from(atob(part), (c) => c.charCodeAt(0)),
      ),
    );
  } catch {
    return null;
  }
}

export function SessionProvider({ children }) {
  const [token, setToken] = useState(read);
  const tokenRef = useRef(token);
  const [notice, setNotice] = useState("");
  const logout = useCallback(() => {
    tokenRef.current = null;
    persist(null);
    setToken(null);
  }, []);
  const expire = useCallback(() => {
    logout();
    setNotice("Sua sessão expirou. Entre novamente para continuar.");
  }, [logout]);
  const api = useMemo(
    () =>
      createApi({
        getToken: () => tokenRef.current,
        onExpired: expire,
        base: import.meta.env.VITE_API_URL || "",
      }),
    [expire],
  );

  useEffect(() => {
    if (!token) return;
    const payload = claims(token);
    const delay = payload?.exp * 1000 - Date.now();
    const timer = setTimeout(
      expire,
      Number.isFinite(delay) ? Math.max(0, delay) : 0,
    );
    return () => clearTimeout(timer);
  }, [token, expire]);

  async function login(email, password) {
    const data = await api("/api/v1/authenticate", {
      method: "POST",
      authenticated: false,
      body: { username: email.trim(), password },
    });
    if (!data?.token || !claims(data.token)?.exp)
      throw new Error("O servidor não retornou uma sessão válida.");
    tokenRef.current = data.token;
    persist(data.token);
    setNotice("");
    setToken(data.token);
  }

  return (
    <Context.Provider
      value={{
        token,
        api,
        login,
        logout,
        notice,
        email: claims(token)?.sub || "",
      }}
    >
      {children}
    </Context.Provider>
  );
}

export function useSession() {
  return useContext(Context);
}
