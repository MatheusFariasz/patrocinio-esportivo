# Patrocínio esportivo — Arena

Sistema de gestão de patrocínios desenvolvido para a disciplina de Verificação, Validação e Teste de Software do IFSP São Carlos.

Permite criar propostas, avaliar contratos, registrar pagamentos e exposição, acompanhar metas e encerrar ou renovar parcerias.

## Como executar

Instale **JDK 21**, **Node.js 22.12 ou superior** e npm. Não é necessário instalar Maven: o projeto inclui o Maven Wrapper. No IntelliJ, configure o SDK do projeto e o JDK do Maven como **21**.

Abra dois terminais na pasta do projeto (`patrocinio-esportivo`). No primeiro, inicie a API:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

No segundo, inicie a interface:

```powershell
cd frontend
npm ci
npm run dev
```

Mantenha os dois terminais abertos. Para parar, pressione `Ctrl+C` em cada um. Em Linux/macOS, substitua `.\mvnw.cmd` por `./mvnw`.

| Acesso | Endereço |
|---|---|
| Interface | http://127.0.0.1:5173 |
| API | http://localhost:8080/api/v1 |
| Swagger (documentação interativa da API) | http://localhost:8080/api/v1/api-docs |
| Especificação OpenAPI | http://localhost:8080/api/v1/openapi |

O front-end encaminha as chamadas `/api` à API na porta 8080. Para outro endereço, consulte [a configuração do front-end](frontend/README.md).

## Contas e permissões

Não existe conta ou senha padrão. Abra a interface, escolha **Criar conta** e cadastre seu nome, sobrenome, e-mail e senha. Use esse e-mail e essa senha para entrar depois.

| Ação | Comercial (`USER`) | Diretor financeiro (`ADMIN`) |
|---|---|---|
| Consultar contratos e histórico | Sim | Sim |
| Criar propostas | Sim | Sim |
| Editar ou cancelar propostas pendentes | Sim | Sim |
| Aprovar ou recusar propostas pendentes | Não | Sim |
| Registrar pagamentos e exposição | Sim | Sim |
| Renovar ou encerrar contratos | Sim | Sim |

As operações também dependem do estado do contrato e das regras de negócio. A autorização é conferida no back-end. Atualmente, os contratos não são separados por dono da conta: usuários autenticados podem consultar um contrato conhecido pelo número.

### Preparar uma conta financeira local

Todo cadastro recebe o perfil `USER`. **O e-mail `financeiro@exemplo.com`, sozinho, não concede privilégios.**

1. Cadastre essa conta pela interface, escolhendo uma senha.
2. Abra o banco SQLite usado pela API em um cliente como o DB Browser for SQLite.
3. Execute o SQL abaixo e salve as alterações no banco:

```sql
UPDATE app_user
SET role = 'ADMIN'
WHERE email = 'financeiro@exemplo.com';

SELECT email, role
FROM app_user
WHERE email = 'financeiro@exemplo.com';
```

4. Confirme que a consulta mostra `ADMIN`. Saia da conta na interface e entre novamente.

Você pode substituir o e-mail no SQL pelo de outra conta já cadastrada. A interface não oferece alteração de perfil.

## Banco de dados e dados de demonstração

Por padrão, o banco é o arquivo **`database.db` na raiz do projeto**, criado na inicialização. O perfil `demo` adiciona, se ainda não existirem:

| Registro | Número (ID) |
|---|---:|
| Clube de demonstração | 10 |
| Patrocinador de demonstração | 20 |

Esses registros não incluem contas de usuário. Os dados cadastrados permanecem no banco após reiniciar a API.

Para usar um banco separado em testes manuais, execute na raiz, no lugar do comando normal da API:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080 --spring.profiles.active=demo --spring.datasource.url=jdbc:sqlite:target/frontend-smoke.db"
```

Esse comando usa `target/frontend-smoke.db` e não o `database.db`. Contas e perfis pertencem a cada banco: uma conta promovida em um arquivo não é promovida no outro. A pasta `target/` é temporária e é removida por `mvn clean`.

## Primeiro uso

1. Entre com uma conta comercial e abra **Nova proposta**.
2. Informe clube **10**, patrocinador **20**, valor e meta maiores que zero. Escolha início e término válidos, com término depois do início e sem período já encerrado.
3. Crie a proposta e guarde seu número. Ela começa como **Pendente** e pode ser editada ou cancelada.
4. Entre com uma conta financeira, consulte esse número na **Visão geral** e aprove ou recuse a proposta.
5. Ao aprovar, o contrato fica **Ativo** e suas parcelas aparecem. Registre os pagamentos e a exposição nessa tela.
6. O encerramento mostra a multa calculada pelo servidor. A renovação exige período encerrado e parcelas quitadas; com a meta atingida, abre um novo período e guarda o anterior no histórico. Sem atingir a meta, o contrato fica **Em risco**, sem abrir novo período.

A interface oferece consulta por número e cartões de acessos recentes, salvos por conta neste navegador. Não há listagem global de propostas/contratos nem telas de listagem ou cadastro de clubes e patrocinadores.

## Testes

Na raiz do projeto, com Java 21:

```powershell
.\mvnw.cmd test
```

Para executar uma suíte específica, escolha um dos comandos:

```powershell
.\mvnw.cmd "-Dtest=TddSuite" test
.\mvnw.cmd "-Dtest=FunctionalSuite" test
.\mvnw.cmd "-Dtest=UnitTestSuite" test
```

Para cobertura de branches, execute `UnitTestSuite` com **Run with Coverage** no IntelliJ. Para mutação, siga os comandos e os resultados registrados em [Mutation.md](Mutation.md).

Na pasta `frontend`, execute:

```powershell
npm test
npm run build
npm run preview
```

O build fica em `frontend/dist/`. O preview abre em http://127.0.0.1:4173 e precisa da API em execução.

### Validação realizada

A última execução completa do back-end teve **266 testes aprovados**. A execução de mutação documentada matou **233 de 233 mutantes**. O front-end passou nos **4 testes automatizados do cliente HTTP** e no build.

Nos testes manuais pelo Chrome foram verificados cadastro, login e logout, criação e edição de proposta, bloqueio de aprovação para comercial, aprovação financeira, pagamento, exposição, encerramento com multa, cancelamento, recusa, consulta de histórico e layout no celular. Também foram conferidos senha incorreta, e-mail repetido, contrato inexistente e bloqueio de renovação antecipada.

**A renovação bem-sucedida ainda não foi validada pela interface nessa rodada.** O erro de e-mail já cadastrado aparece em inglês. Os resultados acima descrevem a execução realizada; não significam que todos os cenários possíveis foram testados.

## Onde encontrar cada parte

Todos os caminhos abaixo são relativos à raiz do repositório.

| Caminho | Conteúdo |
|---|---|
| `src/main/java/` | Back-end: domínio, serviços, repositórios, segurança e controladores |
| `src/main/resources/` | Configuração, estrutura do banco e dados de demonstração |
| `src/test/java/` | Testes e suítes Java |
| `frontend/src/` | Páginas, componentes, sessão, cliente HTTP e estilos |
| `frontend/tests/` | Testes do cliente HTTP |
| `database.db` | Banco local padrão |
| `Mutation.md` | Relatório e reprodução dos testes de mutação |
| `frontend/README.md` | Rotas da interface, configuração e publicação |

## Problemas comuns

- **Erro de compilação com `javac 25`:** confira se o projeto, o Maven e o terminal estão usando JDK 21 (`java -version` e `.\mvnw.cmd -v`).
- **Falha de conexão na interface:** confira se a API iniciou na porta 8080 e se os dois terminais continuam abertos.
- **Porta 5173 ou 4173 ocupada:** encerre a outra instância do front-end; a configuração exige essas portas.
- **Clube ou patrocinador não encontrado:** inicie a API com o perfil `demo` e use os IDs 10 e 20.
- **Conta financeira sem permissão:** confira o `role` no banco que a API está usando e entre novamente após a alteração.
- **Conta não encontrada após trocar o comando da API:** confira se você passou a usar outro arquivo SQLite.
