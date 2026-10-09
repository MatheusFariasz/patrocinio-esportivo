# Patrocínio esportivo

Projeto Java 21 e Spring Boot para gestão de contratos de patrocínio esportivo.

## Autenticação

A persistência dos usuários utiliza SQLite e `JdbcTemplate`, sem JPA ou Lombok.
O arquivo `schema.sql` cria a tabela `app_user` e o índice de email único caso ainda
não existam. A inicialização não apaga os usuários cadastrados.

- `POST /api/v1/register`: recebe `name`, `lastname`, `email` e `password`; retorna o ID com HTTP 201.
- `POST /api/v1/authenticate`: recebe `username` (email) e `password`; retorna `token` com HTTP 200.
- `GET /api/v1/hello`: demonstra uma rota protegida, usando `Authorization: Bearer <token>`.

Senhas são armazenadas com BCrypt. Email duplicado retorna HTTP 409 e credenciais
inválidas retornam HTTP 401.

## Verificação no Windows

Configure `JAVA_HOME` para o JDK 21. Execute na raiz do projeto:

```powershell
.\mvnw.cmd "-Dtest=AuthenticationIntegrationTest" test
.\mvnw.cmd "-Dtest=FunctionalSuite" test
.\mvnw.cmd "-Dtest=TddSuite" test
.\mvnw.cmd "-Dtest=UnitTestSuite" test
```

O teste de autenticação usa Spring Security, HTTP via MockMvc e um arquivo SQLite
temporário. Uma conexão independente verifica a persistência e a reaplicação do
schema sem perda de dados. Sua tag é `IntegrationTest`, pois ele não é unitário.
As suítes do domínio mantêm as tags `UnitTest`, `TDD` e `Functional`.

## Integração da API de negócio

A autenticação está validada em um contexto Spring limitado aos seus componentes.
A aplicação completa e `DemoAuthAppApplicationTests.contextLoads` ainda dependem
das implementações dos repositórios de domínio; atualmente falta o bean de
`ContratoDePatrocinioRepository`.

Após integrar as correções dos serviços, os próximos passos são persistir contratos
e parcelas em SQLite, conectar as referências de clube e patrocinador e expor os
serviços em controllers REST com DTOs, validação e tratamento dos erros de domínio.
