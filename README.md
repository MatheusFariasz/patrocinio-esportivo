# Patrocínio esportivo

Projeto de gestão de patrocínios esportivos desenvolvido para a disciplina **VERIFICAÇÃO, VALIDAÇÃO E TESTE DE SOFTWARE**.

O sistema permite criar e avaliar propostas, acompanhar contratos, registrar pagamentos e exposição e renovar ou encerrar parcerias.

| Parte | Tecnologias | Local |
|---|---|---|
| Back-end | Java 21, Spring Boot, JDBC, SQLite e JWT | `src/` |
| Front-end | React, Vite e React Router | [`frontend/`](frontend/) |

## Executar localmente

Pré-requisitos: **JDK 21**, **Node.js 22.12 ou superior** e npm. O Maven Wrapper está incluído.

No primeiro terminal, na raiz:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

O back-end inicia em `http://localhost:8080`. O perfil `demo` cadastra o clube **10** e o patrocinador **20**. O banco é o arquivo `database.db`, criado automaticamente na raiz.

No segundo terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Abra **http://127.0.0.1:5173**, crie uma conta e entre. Para a primeira proposta, utilize o clube 10 e o patrocinador 20. Guarde o número gerado para consultar o contrato.

Em Linux/macOS, use `./mvnw` no lugar de `.\mvnw.cmd`.

O front-end encaminha `/api` ao back-end durante o desenvolvimento. Não é necessário configurar CORS para esse uso.

### Perfil financeiro

O cadastro cria usuários comerciais (`USER`). Aprovar e recusar propostas exige um diretor financeiro (`ADMIN`); a autorização é conferida pelo back-end.

Para preparar uma conta financeira **no ambiente local**, cadastre-a pela interface e, usando um cliente SQLite no `database.db`, execute:

```sql
UPDATE app_user SET role = 'ADMIN' WHERE email = 'financeiro@exemplo.com';
```

Substitua o e-mail pelo da conta cadastrada e entre novamente. O front-end não permite escolher ou elevar o próprio perfil.

## Testes

```powershell
.\mvnw.cmd test
.\mvnw.cmd "-Dtest=UnitTestSuite,TddSuite,FunctionalSuite" test
.\mvnw.cmd test-compile org.pitest:pitest-maven:mutationCoverage
```

Para cobertura de branches, execute `UnitTestSuite` com **Run with Coverage** no IntelliJ. Os resultados de mutação estão documentados em [`Mutation.md`](Mutation.md).

Testes e build do front-end:

```powershell
cd frontend
npm test
npm run build
npm run preview
```

O preview abre em `http://127.0.0.1:4173`. Instruções de configuração, funcionalidades e publicação: [`frontend/README.md`](frontend/README.md).

Swagger: **http://localhost:8080/api/v1/api-docs**.
