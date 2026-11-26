package br.ifsp.edu.scl.patrocinioesportivo.api;

import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.support.BaseApiIntegrationTest;
import br.ifsp.edu.scl.patrocinioesportivo.support.ContratoTestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("Testes de API - Propostas de Patrocínio (submissão e edição)")
class PropostaApiTest extends BaseApiIntegrationTest {

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("POST /api/v1/propostas deve retornar 201 com header Location e persistir proposta PENDENTE")
    void shouldSubmitProposalReturning201WithLocationHeaderAndPersistingPendingContract(Role role) {
        String token = authenticateAs(role);
        LocalDate inicio = LocalDate.now();
        LocalDate termino = inicio.plusMonths(4);

        Response response = givenAuth(token)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                        "valor", new BigDecimal("2500.75"),
                        "inicio", inicio.toString(),
                        "termino", termino.toString(),
                        "meta", new BigDecimal("180.50")
                ))
                .post("/api/v1/propostas");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        var json = response.jsonPath();
        long propostaId = json.getLong("id");
        var persistida = contratoRepository.buscarPorId(propostaId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(response.getHeader("Location")).isEqualTo("/api/v1/contratos/" + propostaId);
            softly.assertThat(propostaId).isPositive();
            softly.assertThat(json.getLong("clubeId")).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(json.getLong("patrocinadorId")).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(json.getString("status")).isEqualTo("PENDENTE");
            softly.assertThat(json.getString("inicio")).isEqualTo(inicio.toString());
            softly.assertThat(json.getString("termino")).isEqualTo(termino.toString());
            softly.assertThat(json.getObject("valorTotal", BigDecimal.class)).isEqualByComparingTo("2500.75");
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("180.50");
            softly.assertThat(json.getObject("exposicaoAcumulada", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getObject("multaRescisoria", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getList("parcelas")).isEmpty();
            softly.assertThat(json.getList("historico")).isEmpty();

            softly.assertThat(persistida.getStatus()).isEqualTo(StatusContrato.PENDENTE);
            softly.assertThat(persistida.getValorTotal()).isEqualByComparingTo("2500.75");
            softly.assertThat(persistida.getMetaContratual().valor()).isEqualByComparingTo("180.50");
            softly.assertThat(persistida.getPeriodoContratual().inicio()).isEqualTo(inicio);
            softly.assertThat(persistida.getPeriodoContratual().termino()).isEqualTo(termino);
        });
    }

    @Test
    @DisplayName("POST /api/v1/propostas deve retornar 400 para campos invalidos, JSON malformado, partes iguais e periodo invalido")
    void shouldRejectProposalSubmissionWithBadRequestForInvalidPayloadAndDomainRules() {
        String userToken = authenticateAs(Role.USER);
        LocalDate hoje = LocalDate.now();
        LocalDate terminoValido = hoje.plusMonths(3);

        Response payloadVazio = givenAuth(userToken)
                .body("{}")
                .post("/api/v1/propostas");
        Response clubeNaoPositivo = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", 0L,
                        "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                        "valor", new BigDecimal("1000.00"),
                        "inicio", hoje.toString(),
                        "termino", terminoValido.toString(),
                        "meta", new BigDecimal("100.00")
                ))
                .post("/api/v1/propostas");
        Response patrocinadorNegativo = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", -1L,
                        "valor", new BigDecimal("1000.00"),
                        "inicio", hoje.toString(),
                        "termino", terminoValido.toString(),
                        "meta", new BigDecimal("100.00")
                ))
                .post("/api/v1/propostas");
        Response valorZero = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                        "valor", BigDecimal.ZERO,
                        "inicio", hoje.toString(),
                        "termino", terminoValido.toString(),
                        "meta", new BigDecimal("100.00")
                ))
                .post("/api/v1/propostas");
        Response metaNegativa = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                        "valor", new BigDecimal("1000.00"),
                        "inicio", hoje.toString(),
                        "termino", terminoValido.toString(),
                        "meta", new BigDecimal("-10.00")
                ))
                .post("/api/v1/propostas");
        Response dataMalformada = givenAuth(userToken)
                .body("""
                        {
                          "clubeId": 10,
                          "patrocinadorId": 20,
                          "valor": 1000.00,
                          "inicio": "2026-99-99",
                          "termino": "2027-01-01",
                          "meta": 100.00
                        }
                        """)
                .post("/api/v1/propostas");
        Response partesIguais = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "valor", new BigDecimal("1000.00"),
                        "inicio", hoje.toString(),
                        "termino", terminoValido.toString(),
                        "meta", new BigDecimal("100.00")
                ))
                .post("/api/v1/propostas");
        Response terminoIgualInicio = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                        "valor", new BigDecimal("1000.00"),
                        "inicio", hoje.toString(),
                        "termino", hoje.toString(),
                        "meta", new BigDecimal("100.00")
                ))
                .post("/api/v1/propostas");
        Response terminoNoPassado = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                        "valor", new BigDecimal("1000.00"),
                        "inicio", hoje.minusMonths(2).toString(),
                        "termino", hoje.minusDays(1).toString(),
                        "meta", new BigDecimal("100.00")
                ))
                .post("/api/v1/propostas");

        assertSoftly(softly -> {
            assertApiError(softly, payloadVazio, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "clubeId:");
            assertApiError(softly, clubeNaoPositivo, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "clubeId:");
            assertApiError(softly, patrocinadorNegativo, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "patrocinadorId:");
            assertApiError(softly, valorZero, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "valor:");
            assertApiError(softly, metaNegativa, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "meta:");
            assertApiError(
                    softly,
                    dataMalformada,
                    HttpStatus.BAD_REQUEST,
                    "HttpMessageNotReadableException",
                    "Requisição inválida"
            );
            assertApiError(
                    softly,
                    partesIguais,
                    HttpStatus.BAD_REQUEST,
                    "PartesIguaisError",
                    "partes diferentes"
            );
            assertApiError(
                    softly,
                    terminoIgualInicio,
                    HttpStatus.BAD_REQUEST,
                    "PeriodoInvalidoError",
                    "período contratual é inválido"
            );
            assertApiError(
                    softly,
                    terminoNoPassado,
                    HttpStatus.BAD_REQUEST,
                    "PeriodoInvalidoError",
                    "período contratual é inválido"
            );
        });
    }

    @Test
    @DisplayName("POST /api/v1/propostas deve retornar 401 sem token valido e 404 para clube ou patrocinador inexistente")
    void shouldRejectProposalSubmissionForUnauthenticatedAndNonExistentParties() {
        String userToken = authenticateAs(Role.USER);
        LocalDate inicio = LocalDate.now();
        LocalDate termino = inicio.plusMonths(3);

        Map<String, Object> bodyValido = Map.of(
                "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                "valor", new BigDecimal("1000.00"),
                "inicio", inicio.toString(),
                "termino", termino.toString(),
                "meta", new BigDecimal("100.00")
        );

        Response semToken = givenAnonymous()
                .body(bodyValido)
                .post("/api/v1/propostas");
        Response tokenInvalido = givenAuth("token-jwt-invalido")
                .body(bodyValido)
                .post("/api/v1/propostas");
        Response clubeInexistente = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", 999999L,
                        "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                        "valor", new BigDecimal("1000.00"),
                        "inicio", inicio.toString(),
                        "termino", termino.toString(),
                        "meta", new BigDecimal("100.00")
                ))
                .post("/api/v1/propostas");
        Response patrocinadorInexistente = givenAuth(userToken)
                .body(Map.of(
                        "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                        "patrocinadorId", 999999L,
                        "valor", new BigDecimal("1000.00"),
                        "inicio", inicio.toString(),
                        "termino", termino.toString(),
                        "meta", new BigDecimal("100.00")
                ))
                .post("/api/v1/propostas");

        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            softly.assertThat(tokenInvalido.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertApiError(
                    softly,
                    clubeInexistente,
                    HttpStatus.NOT_FOUND,
                    "ClubeInexistenteError",
                    "Clube não encontrado"
            );
            assertApiError(
                    softly,
                    patrocinadorInexistente,
                    HttpStatus.NOT_FOUND,
                    "PatrocinadorInexistenteError",
                    "Patrocinador não encontrado"
            );
        });
    }

    @Test
    @DisplayName("PUT /api/v1/propostas/{id} deve retornar 200 e persistir novos dados da proposta PENDENTE")
    void shouldEditPendingProposalReturning200AndPersistingUpdatedValues() {
        String userToken = authenticateAs(Role.USER);
        long propostaId = criarPropostaPendente(userToken, new BigDecimal("1000.00"), 2, new BigDecimal("100.00"));

        LocalDate novoInicio = LocalDate.now().plusDays(5);
        LocalDate novoTermino = novoInicio.plusMonths(6);

        Response response = givenAuth(userToken)
                .body(Map.of(
                        "valor", new BigDecimal("3750.90"),
                        "inicio", novoInicio.toString(),
                        "termino", novoTermino.toString(),
                        "meta", new BigDecimal("420.25")
                ))
                .put("/api/v1/propostas/{id}", propostaId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        var json = response.jsonPath();
        var persistida = contratoRepository.buscarPorId(propostaId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(json.getLong("id")).isEqualTo(propostaId);
            softly.assertThat(json.getLong("clubeId")).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(json.getLong("patrocinadorId")).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(json.getString("status")).isEqualTo("PENDENTE");
            softly.assertThat(json.getObject("valorTotal", BigDecimal.class)).isEqualByComparingTo("3750.90");
            softly.assertThat(json.getString("inicio")).isEqualTo(novoInicio.toString());
            softly.assertThat(json.getString("termino")).isEqualTo(novoTermino.toString());
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("420.25");
            softly.assertThat(json.getList("parcelas")).isEmpty();
            softly.assertThat(json.getList("historico")).isEmpty();

            softly.assertThat(persistida.getStatus()).isEqualTo(StatusContrato.PENDENTE);
            softly.assertThat(persistida.getValorTotal()).isEqualByComparingTo("3750.90");
            softly.assertThat(persistida.getPeriodoContratual().inicio()).isEqualTo(novoInicio);
            softly.assertThat(persistida.getPeriodoContratual().termino()).isEqualTo(novoTermino);
            softly.assertThat(persistida.getMetaContratual().valor()).isEqualByComparingTo("420.25");
        });
    }

    @Test
    @DisplayName("PUT /api/v1/propostas/{id} deve retornar 401, 400, 404 e 409 em cenarios invalidos")
    void shouldRejectProposalEditionForInvalidAuthPayloadNotFoundAndEvaluatedStatus() {
        String adminToken = authenticateAs(Role.ADMIN);
        String userToken = authenticateAs(Role.USER);
        LocalDate hoje = LocalDate.now();
        LocalDate terminoValido = hoje.plusMonths(4);

        long pendenteId = criarPropostaPendente(userToken, new BigDecimal("1000.00"), 2, new BigDecimal("100.00"));
        long aprovadaId = criarContratoAtivo(adminToken);

        long recusadaId = criarPropostaPendente(userToken);
        givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/recusa", recusadaId)
                .then()
                .statusCode(HttpStatus.OK.value());

        long canceladaId = criarPropostaPendente(userToken);
        givenAuth(userToken)
                .post("/api/v1/propostas/{id}/cancelamento", canceladaId)
                .then()
                .statusCode(HttpStatus.OK.value());

        Map<String, Object> bodyValido = Map.of(
                "valor", new BigDecimal("2000.00"),
                "inicio", hoje.toString(),
                "termino", terminoValido.toString(),
                "meta", new BigDecimal("150.00")
        );

        Response semToken = givenAnonymous()
                .body(bodyValido)
                .put("/api/v1/propostas/{id}", pendenteId);
        Response payloadVazio = givenAuth(userToken)
                .body("{}")
                .put("/api/v1/propostas/{id}", pendenteId);
        Response valorZero = givenAuth(userToken)
                .body(Map.of(
                        "valor", BigDecimal.ZERO,
                        "inicio", hoje.toString(),
                        "termino", terminoValido.toString(),
                        "meta", new BigDecimal("150.00")
                ))
                .put("/api/v1/propostas/{id}", pendenteId);
        Response metaNegativa = givenAuth(userToken)
                .body(Map.of(
                        "valor", new BigDecimal("2000.00"),
                        "inicio", hoje.toString(),
                        "termino", terminoValido.toString(),
                        "meta", new BigDecimal("-5.00")
                ))
                .put("/api/v1/propostas/{id}", pendenteId);
        Response jsonMalformado = givenAuth(userToken)
                .body("{\"valor\": \"invalido\"}")
                .put("/api/v1/propostas/{id}", pendenteId);
        Response idInvalido = givenAuth(userToken)
                .body(bodyValido)
                .put("/api/v1/propostas/{id}", "invalido");
        Response terminoAntesInicio = givenAuth(userToken)
                .body(Map.of(
                        "valor", new BigDecimal("2000.00"),
                        "inicio", hoje.plusMonths(2).toString(),
                        "termino", hoje.plusMonths(1).toString(),
                        "meta", new BigDecimal("150.00")
                ))
                .put("/api/v1/propostas/{id}", pendenteId);
        Response terminoNoPassado = givenAuth(userToken)
                .body(Map.of(
                        "valor", new BigDecimal("2000.00"),
                        "inicio", hoje.minusMonths(2).toString(),
                        "termino", hoje.minusDays(1).toString(),
                        "meta", new BigDecimal("150.00")
                ))
                .put("/api/v1/propostas/{id}", pendenteId);
        Response inexistente = givenAuth(userToken)
                .body(bodyValido)
                .put("/api/v1/propostas/{id}", 999999L);
        Response editarAprovada = givenAuth(userToken)
                .body(bodyValido)
                .put("/api/v1/propostas/{id}", aprovadaId);
        Response editarRecusada = givenAuth(userToken)
                .body(bodyValido)
                .put("/api/v1/propostas/{id}", recusadaId);
        Response editarCancelada = givenAuth(userToken)
                .body(bodyValido)
                .put("/api/v1/propostas/{id}", canceladaId);

        var persistida = contratoRepository.buscarPorId(pendenteId).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertApiError(softly, payloadVazio, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "valor:");
            assertApiError(softly, valorZero, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "valor:");
            assertApiError(softly, metaNegativa, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "meta:");
            assertApiError(
                    softly,
                    jsonMalformado,
                    HttpStatus.BAD_REQUEST,
                    "HttpMessageNotReadableException",
                    "Requisição inválida"
            );
            assertApiError(
                    softly,
                    idInvalido,
                    HttpStatus.BAD_REQUEST,
                    "MethodArgumentTypeMismatchException",
                    "Requisição inválida"
            );
            assertApiError(
                    softly,
                    terminoAntesInicio,
                    HttpStatus.BAD_REQUEST,
                    "PeriodoInvalidoError",
                    "período contratual é inválido"
            );
            assertApiError(
                    softly,
                    terminoNoPassado,
                    HttpStatus.BAD_REQUEST,
                    "PeriodoInvalidoError",
                    "período contratual é inválido"
            );
            assertApiError(
                    softly,
                    inexistente,
                    HttpStatus.NOT_FOUND,
                    "ContratoInexistenteError",
                    "Proposta não encontrada"
            );
            assertApiError(
                    softly,
                    editarAprovada,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "já avaliadas não podem ser editadas"
            );
            assertApiError(
                    softly,
                    editarRecusada,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "já avaliadas não podem ser editadas"
            );
            assertApiError(
                    softly,
                    editarCancelada,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "já avaliadas não podem ser editadas"
            );
            softly.assertThat(persistida.getValorTotal()).isEqualByComparingTo("1000.00");
        });
    }
}




