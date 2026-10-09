# Patrocínio esportivo

Projeto Java 21 e Spring Boot para gestão de contratos de patrocínio esportivo.
O domínio mantém suas regras de estado, parcelas, metas e renovação. Os controllers
delegam aos serviços; os repositórios usam SQLite e `JdbcTemplate`, sem JPA ou Lombok.

## Executar

Configure `JAVA_HOME` para o JDK 21 e execute na raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

A API inicia em `http://localhost:8080`. O banco padrão é `database.db`, relativo
ao diretório de execução. `schema.sql` cria as tabelas e índices ausentes sem apagar
registros. Para utilizar outro arquivo, informe `spring.datasource.url` nos argumentos.

Para experimentar com referências fictícias de clube e patrocinador:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=demo"
```

O perfil `demo` cadastra o clube `10` e o patrocinador `20`, conservando registros
existentes com esses IDs. Sem esse perfil, cadastre as referências nas tabelas
`clube` e `patrocinador` pelo módulo externo ou pelo editor SQLite. Elas pertencem
a outros agregados e são referenciadas apenas por ID neste módulo.

## Login e permissões

| Método | Caminho | Corpo / resultado |
|---|---|---|
| POST | `/api/v1/register` | `name`, `lastname`, `email`, `password`; retorna `id` com HTTP 201 |
| POST | `/api/v1/authenticate` | `username` (email), `password`; retorna `token` |
| GET | `/api/v1/hello` | Demonstra acesso autenticado |

Nas operações de negócio, envie `Authorization: Bearer <token>`. Senhas são
armazenadas com BCrypt. O cadastro público sempre cria `USER`, associado a
`COMERCIAL`. `ADMIN` representa `DIRETOR_FINANCEIRO`; aprovação e recusa exigem esse
perfil. A permissão é obtida do usuário autenticado, nunca do JSON recebido.

Para preparar um diretor na demonstração, cadastre seu usuário e execute no editor
SQLite do banco utilizado pela aplicação:

```sql
UPDATE app_user SET role = 'ADMIN' WHERE email = 'diretor@exemplo.com';
```

Não há endpoint público para promover um usuário a ADMIN.

## Endpoints de negócio

| Método | Caminho | Operação |
|---|---|---|
| POST | `/api/v1/propostas` | Submeter uma proposta; HTTP 201 e `Location` do contrato |
| PUT | `/api/v1/propostas/{id}` | Editar valor, período e meta de proposta pendente |
| POST | `/api/v1/propostas/{id}/aprovacao` | Aprovar e gerar parcelas |
| POST | `/api/v1/propostas/{id}/recusa` | Recusar proposta |
| POST | `/api/v1/propostas/{id}/cancelamento` | Cancelar proposta pendente |
| GET | `/api/v1/contratos/{id}` | Consultar o contrato, parcelas e histórico |
| POST | `/api/v1/contratos/{id}/parcelas/{numero}/pagamentos` | Registrar pagamento na data atual |
| POST | `/api/v1/contratos/{id}/exposicoes` | Registrar exposição midiática |
| POST | `/api/v1/contratos/{id}/encerramento` | Encerrar e calcular multa sobre parcelas pendentes |
| POST | `/api/v1/contratos/{id}/renovacao` | Avaliar renovação, atualizar período e conservar histórico |

Corpo para submeter uma proposta (use um período válido que não esteja encerrado):

```json
{
  "clubeId": 10,
  "patrocinadorId": 20,
  "valor": 1200.00,
  "inicio": "2026-10-09",
  "termino": "2027-01-09",
  "meta": 100
}
```

Na edição, envie somente `valor`, `inicio`, `termino` e `meta`. Para exposição,
envie `{"valor": 25}`; para renovação, `{"duracaoMeses": 3, "novaMeta": 150}`.
As outras operações POST não exigem corpo. As respostas de negócio devolvem o
estado persistido, incluindo IDs, condições contratuais, parcelas e histórico.
Uma avaliação de renovação com meta não atingida pode retornar HTTP 200 e status
`EM_RISCO`, conforme o comportamento do domínio; confira o estado na resposta.

Erros: entrada inválida `400`, autenticação ausente/inválida `401`, falta de
permissão `403`, recurso inexistente `404` e conflito de estado ou pendência `409`.
Erros tratados pelos controllers usam `message`, `status`, `timestamp` e
`developerMessage` (tipo da exceção, sem stack trace). Rejeições de autenticação
no filtro podem retornar corpo vazio ou a mensagem de JWT inválido.

Swagger: `http://localhost:8080/api/v1/api-docs`.
OpenAPI JSON: `http://localhost:8080/api/v1/openapi`.
Use **Authorize** no Swagger para informar o token retornado pelo login.

## Persistência

Contrato, parcelas atuais e histórico são gravados em uma única transação.
O ciclo `0` em `parcela_pagamento` representa as parcelas atuais; ciclos positivos
correspondem às entradas em `historico_periodo`. A reconstituição conserva estados,
datas de pagamento, exposição, multa e referências externas sem executar novamente
transições de negócio. Dinheiro e metas usam texto decimal no SQLite e `BigDecimal`
em Java para preservar precisão. Chaves estrangeiras são habilitadas por conexão.

## Verificação

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd "-Dtest=FunctionalSuite" test
.\mvnw.cmd "-Dtest=TddSuite" test
.\mvnw.cmd "-Dtest=UnitTestSuite" test
.\mvnw.cmd package
```

Os testes de integração usam bancos SQLite temporários, preservando `database.db`.
Eles verificam autenticação, contexto Spring, fluxos HTTP, persistência de histórico,
precisão decimal, chaves estrangeiras e rollback. Sua tag é `IntegrationTest`;
as suítes do domínio mantêm `UnitTest`, `TDD` e `Functional`.

## Correções aguardando integração

Os endpoints de recusa e exposição já estão conectados aos serviços, mas ainda
dependem das duas correções do Vinicius: `RecusarPropostaService` e
`RegistrarExposicaoService` precisam salvar o contrato após a alteração.
Na versão atual, essas alterações não persistem. Os fluxos de sucesso desses dois
endpoints devem ser verificados novamente depois de integrar os commits dele.
Os testes aprovados da API não certificam esses dois fluxos pendentes.
