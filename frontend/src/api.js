export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export function createApi({
  getToken,
  onExpired,
  base = "",
  fetcher = globalThis.fetch,
}) {
  return async function request(
    path,
    { method = "GET", body, signal, authenticated = true } = {},
  ) {
    const headers = { Accept: "application/json" };
    const token = authenticated ? getToken() : null;
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    let response;
    try {
      response = await fetcher(`${base.replace(/\/$/, "")}${path}`, {
        method,
        headers,
        signal,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch (error) {
      if (error.name === "AbortError") throw error;
      throw new ApiError(
        "Não foi possível conectar ao servidor. Confira sua conexão e tente novamente.",
      );
    }
    const text = await response.text();
    let data;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = null;
    }
    if (response.ok) return data;
    if (response.status === 401 && token) {
      onExpired();
      throw new ApiError(
        "Sua sessão expirou. Entre novamente para continuar.",
        401,
      );
    }
    throw new ApiError(
      data?.message ||
        (response.status === 403
          ? "Seu perfil não tem permissão para realizar esta operação."
          : "Não foi possível concluir a operação. Tente novamente."),
      response.status,
    );
  };
}
