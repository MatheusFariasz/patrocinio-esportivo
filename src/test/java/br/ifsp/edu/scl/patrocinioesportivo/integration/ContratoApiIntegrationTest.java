package br.ifsp.edu.scl.patrocinioesportivo.integration;

import br.ifsp.edu.scl.patrocinioesportivo.model.*;
import br.ifsp.edu.scl.patrocinioesportivo.repository.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.UserRepository;
import br.ifsp.edu.scl.patrocinioesportivo.support.SqliteTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("IntegrationTest")
@DisplayName("API de patrocínio com segurança e SQLite reais")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ContratoApiIntegrationTest {
    private static final String DATABASE_URL = SqliteTestDatabase.create();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE_URL);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthenticationService authentication;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired ContratoDePatrocinioRepository contratos;
    @Autowired ClubeRepository clubes;
    @Autowired PatrocinadorRepository patrocinadores;
    @Autowired JdbcTemplate jdbc;

    private String token(Role role) {
        String email = UUID.randomUUID() + "@teste.com";
        users.save(new User(UUID.randomUUID(), "Pessoa", "Teste", email,
                passwords.encode("Senha123!"), role));
        return authentication.authenticate(new AuthRequest(email, "Senha123!")).token();
    }

    private JsonNode response(MockHttpServletRequestBuilder request, String token, int status) throws Exception {
        var result = mvc.perform(request.header("Authorization", "Bearer " + token))
                .andExpect(status().is(status)).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> conditions() {
        return Map.of("valor", new BigDecimal("1000.01"), "inicio", LocalDate.now(),
                "termino", LocalDate.now().plusMonths(2), "meta", new BigDecimal("100"));
    }

    private long proposal(String token) throws Exception {
        var request = new java.util.HashMap<>(conditions());
        request.put("clubeId", 10);
        request.put("patrocinadorId", 20);
        var result = mvc.perform(post("/api/v1/propostas").header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.clubeId").value(10)).andExpect(jsonPath("$.patrocinadorId").value(20))
                .andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/v1/contratos/" + id);
        return id;
    }

    @Test
    @DisplayName("Criar, editar, consultar e cancelar uma proposta com alterações duráveis")
    void proposalLifecycle() throws Exception {
        String token = token(Role.USER);
        long id = proposal(token);
        var newConditions = Map.of("valor", new BigDecimal("900.09"), "inicio", LocalDate.now(),
                "termino", LocalDate.now().plusMonths(3), "meta", new BigDecimal("250"));
        response(put("/api/v1/propostas/" + id).contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(newConditions)), token, 200);
        var persisted = contratos.buscarPorId(id).orElseThrow();
        assertThat(persisted.getValorTotal()).isEqualByComparingTo("900.09");
        assertThat(persisted.getMetaContratual().valor()).isEqualByComparingTo("250");
        assertThat(persisted.getClubeId()).isEqualTo(10L);
        assertThat(persisted.getPatrocinadorId()).isEqualTo(20L);
        assertThat(response(get("/api/v1/contratos/" + id), token, 200).get("valorTotal").decimalValue())
                .isEqualByComparingTo("900.09");
        response(post("/api/v1/propostas/" + id + "/cancelamento"), token, 200);
        assertThat(contratos.buscarPorId(id).orElseThrow().getStatus()).isEqualTo(StatusContrato.CANCELADO);
        response(post("/api/v1/propostas/" + id + "/cancelamento"), token, 409);
    }

    @Test
    @DisplayName("Aprovar como diretor, pagar parcela e encerrar conservando a multa")
    void approvalPaymentAndTermination() throws Exception {
        String user = token(Role.USER);
        String admin = token(Role.ADMIN);
        long id = proposal(user);
        response(post("/api/v1/propostas/" + id + "/aprovacao"), user, 403);
        response(post("/api/v1/propostas/" + id + "/recusa"), user, 403);
        response(post("/api/v1/propostas/" + id + "/aprovacao"), admin, 200);
        var approved = contratos.buscarPorId(id).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(approved.getParcelas()).hasSize(2);
        assertThat(approved.getParcelas().stream().map(ParcelaDePagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1000.01");
        response(post("/api/v1/contratos/" + id + "/parcelas/1/pagamentos"), user, 200);
        var paid = contratos.buscarPorId(id).orElseThrow().getParcelas().getFirst();
        assertThat(paid.isPaga()).isTrue();
        assertThat(paid.getDataPagamento()).isEqualTo(LocalDate.now());
        response(post("/api/v1/contratos/" + id + "/parcelas/1/pagamentos"), user, 409);
        response(post("/api/v1/contratos/" + id + "/parcelas/999/pagamentos"), user, 404);
        response(post("/api/v1/contratos/" + id + "/encerramento"), user, 200);
        var ended = contratos.buscarPorId(id).orElseThrow();
        assertThat(ended.getStatus()).isEqualTo(StatusContrato.ENCERRADO);
        assertThat(ended.getMultaRescisoria()).isEqualByComparingTo("100.002");
        assertThat(ended.getParcelas().getFirst().isPaga()).isTrue();
    }

    @Test
    @DisplayName("Renovar contrato em risco preservando histórico e numeração contínua das parcelas")
    void renewalAndHistory() throws Exception {
        var oldPeriod = new PeriodoContratual(LocalDate.now().minusMonths(3), LocalDate.now().minusDays(1));
        var paid = ParcelaDePagamento.reconstituir(7, new BigDecimal("600.00"), oldPeriod.termino(),
                true, oldPeriod.termino());
        var original = ContratoDePatrocinio.reconstituir(null, StatusContrato.EM_RISCO, oldPeriod,
                new MetaContratual(new BigDecimal("100")), new BigDecimal("600.00"), 10L, 20L,
                new BigDecimal("110.00"), BigDecimal.ZERO, List.of(paid), List.of());
        contratos.salvar(original);
        response(post("/api/v1/contratos/" + original.getId() + "/renovacao").contentType(APPLICATION_JSON)
                .content("{\"duracaoMeses\":2,\"novaMeta\":200}"), token(Role.USER), 200);
        var loaded = contratos.buscarPorId(original.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(loaded.getPeriodoContratual().inicio()).isEqualTo(oldPeriod.termino().plusDays(1));
        assertThat(loaded.getExposicaoAcumulada()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(loaded.getHistorico()).hasSize(1);
        assertThat(loaded.getHistorico().getFirst().periodo()).isEqualTo(oldPeriod);
        assertThat(loaded.getHistorico().getFirst().exposicaoAcumulada()).isEqualByComparingTo("110");
        assertThat(loaded.getHistorico().getFirst().parcelas().getFirst().getDataPagamento()).isEqualTo(oldPeriod.termino());
        assertThat(loaded.getParcelas()).extracting(ParcelaDePagamento::getNumero).containsExactly(8, 9);
        contratos.salvar(loaded);
        assertThat(contratos.buscarPorId(loaded.getId()).orElseThrow().getHistorico()).hasSize(1);
    }

    @Test
    @DisplayName("Falha ao gravar uma parcela desfaz a atualização do agregado inteiro")
    void rollbackOnInvalidParcel() throws Exception {
        String user = token(Role.USER);
        long id = proposal(user);
        response(post("/api/v1/propostas/" + id + "/aprovacao"), token(Role.ADMIN), 200);
        response(post("/api/v1/contratos/" + id + "/parcelas/1/pagamentos"), user, 200);
        var original = contratos.buscarPorId(id).orElseThrow();
        var invalid = ContratoDePatrocinio.reconstituir(id, StatusContrato.ENCERRADO,
                original.getPeriodoContratual(), original.getMetaContratual(), original.getValorTotal(), 10L, 20L,
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(new ParcelaDePagamento(0, BigDecimal.TEN)), List.of());
        assertThatThrownBy(() -> contratos.salvar(invalid)).isInstanceOf(DataAccessException.class);
        var persisted = contratos.buscarPorId(id).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(persisted.getParcelas()).hasSize(2);
        assertThat(persisted.getParcelas().getFirst().isPaga()).isTrue();
        assertThat(persisted.getParcelas().getFirst().getDataPagamento()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("SQLite preserva decimais extensos e rejeita referências inexistentes")
    void exactDecimalsAndForeignKeys() {
        assertThat(clubes.existePorId(10L)).isTrue();
        assertThat(patrocinadores.existePorId(20L)).isTrue();
        assertThat(clubes.existePorId(999L)).isFalse();
        var amount = new BigDecimal("12345678901234567890.123456789");
        var original = new ContratoDePatrocinio(StatusContrato.PENDENTE,
                new PeriodoContratual(LocalDate.now(), LocalDate.now().plusMonths(1)),
                new MetaContratual(BigDecimal.TEN), amount, 10L, 20L);
        contratos.salvar(original);
        assertThat(contratos.buscarPorId(original.getId()).orElseThrow().getValorTotal()).isEqualTo(amount);
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        var invalid = new ContratoDePatrocinio(StatusContrato.PENDENTE, original.getPeriodoContratual(),
                original.getMetaContratual(), amount, 999L, 20L);
        assertThatThrownBy(() -> contratos.salvar(invalid)).isInstanceOf(DataAccessException.class);
        assertThat(invalid.getId()).isNull();
    }

    @Test
    @DisplayName("API retorna 400, 404 e 409 sem alterar o contrato em requisições inválidas")
    void invalidRequests() throws Exception {
        String user = token(Role.USER);
        mvc.perform(get("/api/v1/contratos/1")).andExpect(status().isUnauthorized());
        response(post("/api/v1/propostas").contentType(APPLICATION_JSON).content("{}"), user, 400);
        response(post("/api/v1/propostas").contentType(APPLICATION_JSON).content("{invalido}"), user, 400);
        response(get("/api/v1/contratos/invalido"), user, 400);
        response(get("/api/v1/contratos/999999"), user, 404);
        long id = proposal(user);
        response(post("/api/v1/contratos/" + id + "/parcelas/1/pagamentos"), user, 409);
        response(post("/api/v1/contratos/" + id + "/renovacao").contentType(APPLICATION_JSON)
                .content("{\"duracaoMeses\":0,\"novaMeta\":200}"), user, 400);
        assertThat(contratos.buscarPorId(id).orElseThrow().getStatus()).isEqualTo(StatusContrato.PENDENTE);
    }

    @Test
    @DisplayName("Recusa autorizada é conservada na resposta e em nova leitura SQLite")
    void persistedRefusal() throws Exception {
        String admin = token(Role.ADMIN);
        long id = proposal(admin);
        var result = response(post("/api/v1/propostas/" + id + "/recusa"), admin, 200);
        assertThat(result.get("status").asText()).isEqualTo("RECUSADO");
        assertThat(contratos.buscarPorId(id).orElseThrow().getStatus()).isEqualTo(StatusContrato.RECUSADO);
        assertThat(response(get("/api/v1/contratos/" + id), admin, 200).get("status").asText())
                .isEqualTo("RECUSADO");
        response(post("/api/v1/propostas/" + id + "/recusa"), admin, 409);
    }

    @Test
    @DisplayName("Exposição acumula no SQLite e entrada inválida não altera o total")
    void persistedExposure() throws Exception {
        String user = token(Role.USER);
        long id = proposal(user);
        response(post("/api/v1/propostas/" + id + "/aprovacao"), token(Role.ADMIN), 200);
        response(post("/api/v1/contratos/" + id + "/exposicoes").contentType(APPLICATION_JSON)
                .content("{\"valor\":25.25}"), user, 200);
        var result = response(post("/api/v1/contratos/" + id + "/exposicoes").contentType(APPLICATION_JSON)
                .content("{\"valor\":10.10}"), user, 200);
        assertThat(result.get("exposicaoAcumulada").decimalValue()).isEqualByComparingTo("35.35");
        assertThat(contratos.buscarPorId(id).orElseThrow().getExposicaoAcumulada())
                .isEqualByComparingTo("35.35");
        response(post("/api/v1/contratos/" + id + "/exposicoes").contentType(APPLICATION_JSON)
                .content("{\"valor\":-1}"), user, 400);
        assertThat(response(get("/api/v1/contratos/" + id), user, 200).get("exposicaoAcumulada").decimalValue())
                .isEqualByComparingTo("35.35");
    }

    @Test
    @DisplayName("OpenAPI disponibiliza os endpoints e o esquema de autenticação Bearer")
    void openApiDocumentation() throws Exception {
        mvc.perform(get("/api/v1/openapi")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/propostas'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/contratos/{id}'].get").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
        var documentation = mvc.perform(get("/api/v1/api-docs")).andExpect(status().is3xxRedirection()).andReturn();
        String location = documentation.getResponse().getHeader("Location");
        assertThat(location).isNotBlank();
        mvc.perform(get(location)).andExpect(status().isOk());
    }
}
