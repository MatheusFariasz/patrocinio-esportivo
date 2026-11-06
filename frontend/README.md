# Arena — front-end

Interface em português feita com React, Vite e React Router. Para iniciar o projeto, criar contas, configurar o perfil financeiro e entender as permissões, siga o [README principal](../README.md).

## Executar

Com a API em execução na porta 8080, abra um terminal na raiz do projeto:

```powershell
cd frontend
npm ci
npm run dev
```

Acesse **http://127.0.0.1:5173**. O cadastro cria uma conta comercial; não há usuário ou senha padrão.

## Páginas e caminhos

| Página | Endereço local |
|---|---|
| Entrar | http://127.0.0.1:5173/#/entrar |
| Criar conta | http://127.0.0.1:5173/#/cadastro |
| Visão geral | http://127.0.0.1:5173/#/ |
| Nova proposta | http://127.0.0.1:5173/#/propostas/nova |
| Consultar contrato | `http://127.0.0.1:5173/#/contratos/1` |
| Editar proposta | `http://127.0.0.1:5173/#/propostas/1/editar` |

Nos dois últimos exemplos, substitua `1` pelo número gerado para sua proposta/contrato. As páginas de trabalho exigem login. A edição depende de a proposta estar pendente.

A Visão geral permite consultar por número e mostra acessos recentes desta conta neste navegador. Não existe listagem global de contratos/propostas nem tela de clubes ou patrocinadores. Para demonstração, o perfil `demo` da API disponibiliza clube 10 e patrocinador 20.

## Contas e sessão

- Toda conta cadastrada começa como comercial (`USER`).
- Aprovar e recusar exigem diretor financeiro (`ADMIN`). A preparação dessa conta local está no [README principal](../README.md#preparar-uma-conta-financeira-local).
- As outras operações dependem das regras e do estado do contrato; as permissões são verificadas na API.
- Um erro 403 mostra a falta de permissão e mantém a sessão. Um erro 401 em chamada autenticada encerra a sessão.
- O token de acesso fica no `sessionStorage`, por aba. Os números acessados recentemente ficam no `localStorage`, separados pelo e-mail. Esses cartões não representam propriedade dos contratos.

## Configurar o endereço da API

Por padrão, o Vite encaminha `/api` para `http://localhost:8080`, tanto no desenvolvimento quanto no preview.

Para mudar esse destino, execute na pasta `frontend`:

```powershell
Copy-Item .env.example .env
```

Edite `frontend/.env`, altere `API_PROXY_TARGET` para o endereço da sua API e reinicie o Vite. Para esse uso local, deixe `VITE_API_URL` vazio.

## Testes e build

Na pasta `frontend`:

```powershell
npm test
npm run build
npm run preview
```

Os 4 testes automatizados verificam o cliente HTTP: acesso negado, sessão recusada, credenciais inválidas e falhas de conexão/cancelamento. Não substituem os testes manuais de todas as telas.

O build é gerado em `frontend/dist/`. O preview abre em **http://127.0.0.1:4173** e precisa da API em execução. Os resultados dos testes manuais e suas limitações estão no [README principal](../README.md#validação-realizada).

## Publicação

1. Execute `npm run build` na pasta `frontend`.
2. Publique o conteúdo de `frontend/dist/` na hospedagem estática.
3. Configure a hospedagem para encaminhar `/api` ao back-end publicado.

As rotas usam hash (`/#/contratos/1`), portanto não exigem reescrita de caminhos de páginas no servidor. O proxy do Vite só funciona no desenvolvimento e no preview.

Se a API estiver em outra origem, uma alternativa é definir `VITE_API_URL` no `.env`, somente com a origem (por exemplo, `https://api.exemplo.com`, sem `/api/v1`), e liberar a origem do front-end por CORS no servidor. Essa configuração depende da hospedagem e da API. Após mudar a variável, gere o build novamente. Use HTTPS para ambos.

A aplicação ainda não foi publicada em uma hospedagem nesta entrega.

## Organização

Caminhos relativos à pasta `frontend`:

```text
src/
  App.jsx         Rotas e estrutura das páginas
  api.js          Cliente HTTP e tratamento de erros
  session.jsx     Sessão JWT
  recent.js       Números consultados no navegador
  components.jsx  Campos, alertas, status e modal
  pages/          Entrada, painel, proposta e contrato
  styles.css      Estilos e responsividade
tests/            Testes do cliente HTTP
vite.config.js    Portas e proxy da API
.env.example      Exemplo de configuração local
```
