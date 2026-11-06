# Arena — front-end de patrocínios esportivos

Interface em português, feita com React, Vite e React Router e integrada aos endpoints existentes do projeto. O visual usa verde, tons claros e uma identidade ligada ao esporte.

## Requisitos

- JDK 21 para o back-end.
- Node.js 22.12 ou superior e npm para o front-end.

## Configurar e executar

1. Na raiz do repositório, inicie o back-end com dados de demonstração:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
   ```

2. Em outro terminal:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

3. Acesse **http://127.0.0.1:5173**, crie uma conta e faça login.

O perfil `demo` disponibiliza o clube **10** e o patrocinador **20**. O cadastro de usuários gera o perfil comercial. A configuração de uma conta financeira local está explicada no README da raiz.

Para outro endereço de back-end, copie `.env.example` para `.env` e altere `API_PROXY_TARGET`. Reinicie o Vite depois da alteração. A configuração padrão é `http://localhost:8080`.

## Funcionalidades

- Cadastro, login, logout e encerramento automático de sessão expirada.
- Consulta de contrato pelo número e acessos recentes separados por conta no navegador.
- Criação e edição de propostas pendentes.
- Aprovação, recusa e cancelamento com confirmação.
- Consulta das parcelas e registro de pagamentos.
- Registro de exposição e acompanhamento da meta contratual.
- Renovação com duração e nova meta; consulta do histórico de períodos.
- Encerramento e visualização da multa calculada pelo servidor.
- Mensagens de validação, permissões, indisponibilidade e operações em andamento.
- Layout adaptado a celular e desktop, campos com rótulos, navegação por teclado e diálogos de confirmação.

O back-end não oferece listagem global de contratos nem cadastro de clubes/patrocinadores pela API. Por isso, a tela inicial consulta por número e mostra apenas os IDs acessados neste navegador. Não são inventados registros ou indicadores de contratos.

Aprovação e recusa exigem o perfil financeiro. Um erro 403 é exibido sem desconectar o usuário. Um erro 401 em uma chamada autenticada encerra a sessão.

O JWT fica no `sessionStorage`, por aba. Somente os identificadores recentes ficam no `localStorage`, separados pelo e-mail. As regras de negócio e a autorização continuam no servidor.

## Testes e build

```powershell
npm test
npm run build
npm run preview
```

Os testes usam o runner do Node e verificam expiração de sessão, acesso negado, login e falhas de conexão. O build é gerado em `dist/`; o preview fica em **http://127.0.0.1:4173** e também encaminha `/api` ao back-end.

## Publicar

Publique a pasta `dist/` como site estático. A navegação usa hash (`/#/contratos/1`), evitando a necessidade de reescrita das rotas de páginas no servidor.

Em produção, o proxy de desenvolvimento do Vite não existe. É necessário encaminhar `/api` ao back-end por um proxy reverso na hospedagem, ou definir `VITE_API_URL` para uma API publicada que autorize a origem do front-end por CORS. Essa variável é incorporada no build; após alterá-la, execute `npm run build` novamente. Use HTTPS para a interface e a API.

Esta entrega contém o código e o build local; a hospedagem não é realizada automaticamente.

## Organização

```text
src/
  api.js          Cliente HTTP e tratamento de erros
  session.jsx     Sessão JWT
  recent.js       Identificadores consultados no navegador
  components.jsx  Campos, alertas, status e modal
  pages/          Entrada, painel, proposta e contrato
  styles.css      Identidade visual e responsividade
```

## Referências

A organização em páginas, cliente HTTP e sessão centralizada foi inspirada nos projetos [Melhor Amigo Animal Care](https://github.com/KaiqueGalera/MelhorAmigoAnimalCare) e [Sistema de Reserva de Barbearia](https://github.com/LucasLotumolo/sistema-reserva-barbearia), indicados pela equipe. A interface e os fluxos foram desenvolvidos para o domínio de patrocínios, com identidade visual própria. O projeto permanece sob a licença GPL-3.0 do repositório.
