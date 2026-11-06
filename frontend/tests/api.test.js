import test from "node:test";
import assert from "node:assert/strict";
import { createApi, ApiError } from "../src/api.js";

test("403 informa falta de permissão sem encerrar a sessão", async () => {
  let expired = false;
  const api = createApi({
    getToken: () => "token",
    onExpired: () => {
      expired = true;
    },
    fetcher: async () =>
      new Response(
        JSON.stringify({
          message: "Apenas o diretor financeiro pode aprovar.",
        }),
        { status: 403 },
      ),
  });
  await assert.rejects(
    api("/api/v1/propostas/1/aprovacao", { method: "POST" }),
    (error) =>
      error instanceof ApiError &&
      error.status === 403 &&
      error.message.includes("diretor financeiro"),
  );
  assert.equal(expired, false);
});

test("401 em uma chamada autenticada encerra a sessão", async () => {
  let expired = false;
  const api = createApi({
    getToken: () => "token",
    onExpired: () => {
      expired = true;
    },
    fetcher: async () => new Response("", { status: 401 }),
  });
  await assert.rejects(
    api("/api/v1/contratos/1"),
    (error) => error.status === 401,
  );
  assert.equal(expired, true);
});

test("login não envia o token anterior e mostra o erro de credenciais", async () => {
  const api = createApi({
    getToken: () => "antigo",
    onExpired: () => assert.fail("Não deve expirar"),
    fetcher: async (_url, options) => {
      assert.equal(options.headers.Authorization, undefined);
      assert.equal(
        options.body,
        JSON.stringify({ username: "ana@teste.com", password: "errada" }),
      );
      return new Response(
        JSON.stringify({ message: "Credenciais inválidas." }),
        { status: 401 },
      );
    },
  });
  await assert.rejects(
    api("/api/v1/authenticate", {
      method: "POST",
      authenticated: false,
      body: { username: "ana@teste.com", password: "errada" },
    }),
    /Credenciais inválidas/,
  );
});

