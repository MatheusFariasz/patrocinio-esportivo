const key = (email) => `arena.recent:${email}`;
export function recentContracts(email) {
  try {
    const list = JSON.parse(localStorage.getItem(key(email)) || "[]");
    return Array.isArray(list)
      ? list.filter((id) => /^[1-9]\d*$/.test(String(id))).slice(0, 8)
      : [];
  } catch {
    return [];
  }
}
export function rememberContract(email, id) {
  try {
    localStorage.setItem(
      key(email),
      JSON.stringify(
        [
          String(id),
          ...recentContracts(email).filter(
            (value) => String(value) !== String(id),
          ),
        ].slice(0, 8),
      ),
    );
  } catch {
    /* Consulta continua sem histórico local. */
  }
}
