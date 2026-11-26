package br.ifsp.edu.scl.patrocinioesportivo.api;

import br.ifsp.edu.scl.patrocinioesportivo.model.HistoricoDePeriodo;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("Testes de API - Contratos de Patrocínio")
class ContratoApiTest extends BaseApiIntegrationTest {

    @Test
    @DisplayName("GET /api/v1/contratos/{id} deve retornar 200 com payload completo, atraso nas parcelas e historico")
    void shouldConsultContratoReturningCompletePayloadWithOverdueAndHistory() {
        String userToken = authenticateAs(Role.USER);
        LocalDate hoje = LocalDate.now();
        LocalDate inicioAtual = hoje.minusMonths(2);
        LocalDate terminoAtual = hoje.plusMonths(2);

        var parcelaAtrasada = ParcelaDePagamento.reconstituir(
                2, new BigDecimal("500.00"), hoje.minusDays(1), false, null
        );
        var parcelaVenceHoje = ParcelaDePagamento.reconstituir(
                3, new BigDecimal("500.00"), hoje, false, null
        );
        var parcelaPagaAtrasada = ParcelaDePagamento.reconstituir(
                4, new BigDecimal("500.00"), hoje.minusDays(5), true, hoje.minusDays(2)
        );
        var parcelaFutura = ParcelaDePagamento.reconstituir(
                5, new BigDecimal("500.00"), hoje.plusDays(15), false, null
        );

        var periodoAnterior = new PeriodoContratual(hoje.minusMonths(6), hoje.minusMonths(2).minusDays(1));
        var parcelaHistorico = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("1000.00"), periodoAnterior.termino(), true, periodoAnterior.termino()
        );
        var historico = new HistoricoDePeriodo(
                periodoAnterior,
                new MetaContratual(new BigDecimal("120.00")),
                new BigDecimal("150.00"),
                List.of(parcelaHistorico)
        );

        long contratoId = salvarContratoReconstituido(
                StatusContrato.ATIVO,
                inicioAtual,
                terminoAtual,
                new BigDecimal("300.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("75.50"),
                List.of(parcelaAtrasada, parcelaVenceHoje, parcelaPagaAtrasada, parcelaFutura),
                List.of(historico)
        );

        Response response = givenAuth(userToken)
                .get("/api/v1/contratos/{id}", contratoId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        var json = response.jsonPath();

        assertSoftly(softly -> {
            softly.assertThat(json.getLong("id")).isEqualTo(contratoId);
            softly.assertThat(json.getLong("clubeId")).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(json.getLong("patrocinadorId")).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(json.getString("status")).isEqualTo("ATIVO");
            softly.assertThat(json.getString("inicio")).isEqualTo(inicioAtual.toString());
            softly.assertThat(json.getString("termino")).isEqualTo(terminoAtual.toString());
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("300.00");
            softly.assertThat(json.getObject("valorTotal", BigDecimal.class)).isEqualByComparingTo("2000.00");
            softly.assertThat(json.getObject("exposicaoAcumulada", BigDecimal.class)).isEqualByComparingTo("75.50");
            softly.assertThat(json.getObject("multaRescisoria", BigDecimal.class)).isEqualByComparingTo("0");

            softly.assertThat(json.getList("parcelas.numero", Integer.class)).containsExactly(2, 3, 4, 5);
            softly.assertThat(json.getList("parcelas.paga", Boolean.class)).containsExactly(false, false, true, false);
            softly.assertThat(json.getList("parcelas.emAtraso", Boolean.class)).containsExactly(true, false, false, false);

            softly.assertThat(json.getList("historico")).hasSize(1);
            softly.assertThat(json.getString("historico[0].inicio")).isEqualTo(periodoAnterior.inicio().toString());
            softly.assertThat(json.getString("historico[0].termino")).isEqualTo(periodoAnterior.termino().toString());
            softly.assertThat(json.getObject("historico[0].meta", BigDecimal.class)).isEqualByComparingTo("120.00");
            softly.assertThat(json.getObject("historico[0].exposicaoAcumulada", BigDecimal.class))
                    .isEqualByComparingTo("150.00");
            softly.assertThat(json.getList("historico[0].parcelas.numero", Integer.class)).containsExactly(1);
        });
    }

    @Test
    @DisplayName("GET /api/v1/contratos/{id} deve retornar 401 sem token valido, 400 para ID invalido e 404 para contrato inexistente")
    void shouldRejectContractConsultationForUnauthenticatedMalformedAndNonExistentId() {
        String userToken = authenticateAs(Role.USER);

        Response semToken = givenAnonymous()
                .get("/api/v1/contratos/{id}", 1L);
        Response tokenInvalido = givenAuth("token-jwt-invalido")
                .get("/api/v1/contratos/{id}", 1L);
        Response idInvalido = givenAuth(userToken)
                .get("/api/v1/contratos/{id}", "invalido");
        Response inexistente = givenAuth(userToken)
                .get("/api/v1/contratos/{id}", 999999L);

        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            softly.assertThat(tokenInvalido.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertApiError(
                    softly,
                    idInvalido,
                    HttpStatus.BAD_REQUEST,
                    "MethodArgumentTypeMismatchException",
                    "Requisição inválida"
            );
            assertApiError(
                    softly,
                    inexistente,
                    HttpStatus.NOT_FOUND,
                    "ContratoInexistenteError",
                    "Contrato não encontrado"
            );
        });
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"ATIVO", "EM_RISCO"})
    @DisplayName("POST /api/v1/contratos/{id}/parcelas/{numero}/pagamentos deve retornar 200 e quitar parcela atrasada em contrato ATIVO ou EM_RISCO")
    void shouldRegisterInstallmentPaymentForActiveAndAtRiskContracts(StatusContrato status) {
        String userToken = authenticateAs(Role.USER);
        LocalDate hoje = LocalDate.now();

        var parcelaAtrasada = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("500.00"), hoje.minusDays(3), false, null
        );
        var parcelaFutura = ParcelaDePagamento.reconstituir(
                2, new BigDecimal("500.00"), hoje.plusDays(25), false, null
        );
        long contratoId = salvarContratoReconstituido(
                status,
                hoje.minusMonths(1),
                hoje.plusMonths(1),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                List.of(parcelaAtrasada, parcelaFutura),
                List.of()
        );

        Response response = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", contratoId, 1);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        var json = response.jsonPath();
        var persistido = contratoRepository.buscarPorId(contratoId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(json.getLong("id")).isEqualTo(contratoId);
            softly.assertThat(json.getString("status")).isEqualTo(status.name());
            softly.assertThat(json.getInt("parcelas[0].numero")).isEqualTo(1);
            softly.assertThat(json.getBoolean("parcelas[0].paga")).isTrue();
            softly.assertThat(json.getString("parcelas[0].dataPagamento")).isEqualTo(hoje.toString());
            softly.assertThat(json.getBoolean("parcelas[0].emAtraso")).isFalse();
            softly.assertThat(json.getInt("parcelas[1].numero")).isEqualTo(2);
            softly.assertThat(json.getBoolean("parcelas[1].paga")).isFalse();
            softly.assertThat(json.getString("parcelas[1].dataPagamento")).isNull();

            softly.assertThat(persistido.getParcelas().get(0).isPaga()).isTrue();
            softly.assertThat(persistido.getParcelas().get(0).getDataPagamento()).isEqualTo(hoje);
            softly.assertThat(persistido.getParcelas().get(1).isPaga()).isFalse();
        });
    }

    @Test
    @DisplayName("POST /api/v1/contratos/{id}/parcelas/{numero}/pagamentos deve retornar 401, 400, 404 e 409 em cenarios invalidos")
    void shouldRejectInstallmentPaymentForInvalidAuthParamsNotFoundAndConflictCases() {
        String adminToken = authenticateAs(Role.ADMIN);
        String userToken = authenticateAs(Role.USER);
        long contratoAtivoId = criarContratoAtivo(adminToken);
        long pendenteId = criarPropostaPendente(userToken);

        LocalDate hoje = LocalDate.now();
        long encerradoId = salvarContratoReconstituido(
                StatusContrato.ENCERRADO,
                hoje.minusMonths(2),
                hoje.plusMonths(1),
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                List.of(ParcelaDePagamento.reconstituir(1, new BigDecimal("500.00"), hoje.plusDays(10), false, null)),
                List.of()
        );

        givenAuth(userToken)
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", contratoAtivoId, 1)
                .then()
                .statusCode(HttpStatus.OK.value());

        Response semToken = givenAnonymous()
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", contratoAtivoId, 2);
        Response numeroInvalido = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", contratoAtivoId, "abc");
        Response contratoInexistente = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", 999999L, 1);
        Response parcelaInexistente = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", contratoAtivoId, 99);
        Response parcelaJaPaga = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", contratoAtivoId, 1);
        Response contratoPendente = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", pendenteId, 1);
        Response contratoEncerrado = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/parcelas/{numero}/pagamentos", encerradoId, 1);

        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertApiError(
                    softly,
                    numeroInvalido,
                    HttpStatus.BAD_REQUEST,
                    "MethodArgumentTypeMismatchException",
                    "Requisição inválida"
            );
            assertApiError(
                    softly,
                    contratoInexistente,
                    HttpStatus.NOT_FOUND,
                    "ContratoInexistenteError",
                    "Contrato não encontrado"
            );
            assertApiError(
                    softly,
                    parcelaInexistente,
                    HttpStatus.NOT_FOUND,
                    "ParcelaInexistenteError",
                    "Parcela não encontrada"
            );
            assertApiError(
                    softly,
                    parcelaJaPaga,
                    HttpStatus.CONFLICT,
                    "PagamentoJaRegistradoError",
                    "já foi registrado"
            );
            assertApiError(
                    softly,
                    contratoPendente,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "não permite registrar pagamentos"
            );
            assertApiError(
                    softly,
                    contratoEncerrado,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "não permite registrar pagamentos"
            );
        });
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"ATIVO", "EM_RISCO"})
    @DisplayName("POST /api/v1/contratos/{id}/exposicoes deve retornar 200 e acumular exposicao em contrato ATIVO ou EM_RISCO")
    void shouldRegisterAndAccumulateMediaExposureForActiveAndAtRiskContracts(StatusContrato status) {
        String userToken = authenticateAs(Role.USER);
        LocalDate hoje = LocalDate.now();
        long contratoId = salvarContratoReconstituido(
                status,
                hoje.minusMonths(1),
                hoje.plusMonths(2),
                new BigDecimal("100.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                List.of(ParcelaDePagamento.reconstituir(1, new BigDecimal("1000.00"), hoje.plusDays(15), false, null)),
                List.of()
        );

        Response primeira = givenAuth(userToken)
                .body(Map.of("valor", new BigDecimal("35.50")))
                .post("/api/v1/contratos/{id}/exposicoes", contratoId);
        assertThat(primeira.statusCode()).isEqualTo(HttpStatus.OK.value());

        Response segunda = givenAuth(userToken)
                .body(Map.of("valor", new BigDecimal("64.75")))
                .post("/api/v1/contratos/{id}/exposicoes", contratoId);
        assertThat(segunda.statusCode()).isEqualTo(HttpStatus.OK.value());

        var persistido = contratoRepository.buscarPorId(contratoId).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(primeira.jsonPath().getObject("exposicaoAcumulada", BigDecimal.class))
                    .isEqualByComparingTo("35.50");
            softly.assertThat(segunda.jsonPath().getLong("id")).isEqualTo(contratoId);
            softly.assertThat(segunda.jsonPath().getString("status")).isEqualTo(status.name());
            softly.assertThat(segunda.jsonPath().getObject("exposicaoAcumulada", BigDecimal.class))
                    .isEqualByComparingTo("100.25");
            softly.assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo("100.25");
        });
    }

    @Test
    @DisplayName("POST /api/v1/contratos/{id}/exposicoes deve retornar 401, 400, 404 e 409 em cenarios invalidos sem alterar exposicao")
    void shouldRejectMediaExposureForInvalidAuthPayloadNotFoundAndConflictCases() {
        String adminToken = authenticateAs(Role.ADMIN);
        String userToken = authenticateAs(Role.USER);
        long contratoAtivoId = criarContratoAtivo(adminToken);
        long pendenteId = criarPropostaPendente(userToken);

        LocalDate hoje = LocalDate.now();
        long encerradoId = salvarContratoReconstituido(
                StatusContrato.ENCERRADO,
                hoje.minusMonths(2),
                hoje.plusMonths(1),
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                List.of(ParcelaDePagamento.reconstituir(1, new BigDecimal("500.00"), hoje.plusDays(10), false, null)),
                List.of()
        );

        Response semToken = givenAnonymous()
                .body(Map.of("valor", new BigDecimal("25.00")))
                .post("/api/v1/contratos/{id}/exposicoes", contratoAtivoId);
        Response valorNulo = givenAuth(userToken)
                .body("{}")
                .post("/api/v1/contratos/{id}/exposicoes", contratoAtivoId);
        Response valorZero = givenAuth(userToken)
                .body(Map.of("valor", BigDecimal.ZERO))
                .post("/api/v1/contratos/{id}/exposicoes", contratoAtivoId);
        Response valorNegativo = givenAuth(userToken)
                .body(Map.of("valor", new BigDecimal("-10.50")))
                .post("/api/v1/contratos/{id}/exposicoes", contratoAtivoId);
        Response jsonMalformado = givenAuth(userToken)
                .body("{\"valor\": invalido}")
                .post("/api/v1/contratos/{id}/exposicoes", contratoAtivoId);
        Response contratoInexistente = givenAuth(userToken)
                .body(Map.of("valor", new BigDecimal("25.00")))
                .post("/api/v1/contratos/{id}/exposicoes", 999999L);
        Response contratoPendente = givenAuth(userToken)
                .body(Map.of("valor", new BigDecimal("25.00")))
                .post("/api/v1/contratos/{id}/exposicoes", pendenteId);
        Response contratoEncerrado = givenAuth(userToken)
                .body(Map.of("valor", new BigDecimal("25.00")))
                .post("/api/v1/contratos/{id}/exposicoes", encerradoId);

        var persistido = contratoRepository.buscarPorId(contratoAtivoId).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertApiError(softly, valorNulo, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "valor:");
            assertApiError(softly, valorZero, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "valor:");
            assertApiError(softly, valorNegativo, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "valor:");
            assertApiError(
                    softly,
                    jsonMalformado,
                    HttpStatus.BAD_REQUEST,
                    "HttpMessageNotReadableException",
                    "Requisição inválida"
            );
            assertApiError(
                    softly,
                    contratoInexistente,
                    HttpStatus.NOT_FOUND,
                    "ContratoInexistenteError",
                    "Contrato não encontrado"
            );
            assertApiError(
                    softly,
                    contratoPendente,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "não permite registrar exposição midiática"
            );
            assertApiError(
                    softly,
                    contratoEncerrado,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "não permite registrar exposição midiática"
            );
            softly.assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo("0");
        });
    }

    @Test
    @DisplayName("POST /api/v1/contratos/{id}/encerramento deve retornar 200 com multa de 20% sobre parcelas pendentes e multa zero quando quitado")
    void shouldTerminateContractCalculatingPenaltyForUnpaidAndZeroForFullyPaid() {
        String userToken = authenticateAs(Role.USER);
        LocalDate hoje = LocalDate.now();

        long comPendenciasId = salvarContratoReconstituido(
                StatusContrato.ATIVO,
                hoje.minusMonths(2),
                hoje.plusMonths(2),
                new BigDecimal("200.00"),
                new BigDecimal("1050.00"),
                BigDecimal.ZERO,
                List.of(
                        ParcelaDePagamento.reconstituir(1, new BigDecimal("300.00"), hoje.minusMonths(1), true, hoje.minusMonths(1)),
                        ParcelaDePagamento.reconstituir(2, new BigDecimal("400.00"), hoje.plusDays(10), false, null),
                        ParcelaDePagamento.reconstituir(3, new BigDecimal("350.00"), hoje.plusMonths(1), false, null)
                ),
                List.of()
        );

        long quitadoEmRiscoId = salvarContratoReconstituido(
                StatusContrato.EM_RISCO,
                hoje.minusMonths(2),
                hoje.plusMonths(1),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("50.00"),
                List.of(
                        ParcelaDePagamento.reconstituir(1, new BigDecimal("500.00"), hoje.minusMonths(1), true, hoje.minusMonths(1)),
                        ParcelaDePagamento.reconstituir(2, new BigDecimal("500.00"), hoje, true, hoje)
                ),
                List.of()
        );

        Response respComPendencias = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/encerramento", comPendenciasId);
        assertThat(respComPendencias.statusCode()).isEqualTo(HttpStatus.OK.value());

        Response respQuitado = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/encerramento", quitadoEmRiscoId);
        assertThat(respQuitado.statusCode()).isEqualTo(HttpStatus.OK.value());

        var persistidoComPendencias = contratoRepository.buscarPorId(comPendenciasId).orElseThrow();
        var persistidoQuitado = contratoRepository.buscarPorId(quitadoEmRiscoId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(respComPendencias.jsonPath().getLong("id")).isEqualTo(comPendenciasId);
            softly.assertThat(respComPendencias.jsonPath().getString("status")).isEqualTo("ENCERRADO");
            softly.assertThat(respComPendencias.jsonPath().getObject("multaRescisoria", BigDecimal.class))
                    .isEqualByComparingTo("150.00");
            softly.assertThat(persistidoComPendencias.getStatus()).isEqualTo(StatusContrato.ENCERRADO);
            softly.assertThat(persistidoComPendencias.getMultaRescisoria()).isEqualByComparingTo("150.00");

            softly.assertThat(respQuitado.jsonPath().getLong("id")).isEqualTo(quitadoEmRiscoId);
            softly.assertThat(respQuitado.jsonPath().getString("status")).isEqualTo("ENCERRADO");
            softly.assertThat(respQuitado.jsonPath().getObject("multaRescisoria", BigDecimal.class))
                    .isEqualByComparingTo("0");
            softly.assertThat(persistidoQuitado.getStatus()).isEqualTo(StatusContrato.ENCERRADO);
            softly.assertThat(persistidoQuitado.getMultaRescisoria()).isEqualByComparingTo("0");
        });
    }

    @Test
    @DisplayName("POST /api/v1/contratos/{id}/encerramento deve retornar 401, 404 e 409 em cenarios invalidos")
    void shouldRejectContractTerminationForUnauthorizedNotFoundAndConflictCases() {
        String adminToken = authenticateAs(Role.ADMIN);
        String userToken = authenticateAs(Role.USER);

        long encerradoId = criarContratoAtivo(adminToken);
        givenAuth(userToken)
                .post("/api/v1/contratos/{id}/encerramento", encerradoId)
                .then()
                .statusCode(HttpStatus.OK.value());

        long pendenteId = criarPropostaPendente(userToken);

        long canceladoId = criarPropostaPendente(userToken);
        givenAuth(userToken)
                .post("/api/v1/propostas/{id}/cancelamento", canceladoId)
                .then()
                .statusCode(HttpStatus.OK.value());

        long recusadoId = criarPropostaPendente(userToken);
        givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/recusa", recusadoId)
                .then()
                .statusCode(HttpStatus.OK.value());

        Response semToken = givenAnonymous()
                .post("/api/v1/contratos/{id}/encerramento", encerradoId);
        Response inexistente = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/encerramento", 999999L);
        Response jaEncerrado = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/encerramento", encerradoId);
        Response pendente = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/encerramento", pendenteId);
        Response cancelado = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/encerramento", canceladoId);
        Response recusado = givenAuth(userToken)
                .post("/api/v1/contratos/{id}/encerramento", recusadoId);

        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertApiError(
                    softly,
                    inexistente,
                    HttpStatus.NOT_FOUND,
                    "ContratoInexistenteError",
                    "Contrato não encontrado"
            );
            assertApiError(
                    softly,
                    jaEncerrado,
                    HttpStatus.CONFLICT,
                    "OperacaoRedundanteError",
                    "já está encerrado"
            );
            assertApiError(
                    softly,
                    pendente,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "ativos ou em risco"
            );
            assertApiError(
                    softly,
                    cancelado,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "ativos ou em risco"
            );
            assertApiError(
                    softly,
                    recusado,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "ativos ou em risco"
            );
        });
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"ATIVO", "EM_RISCO"})
    @DisplayName("POST /api/v1/contratos/{id}/renovacao deve retornar 200, renovar ciclo, arquivar historico e continuar numeracao de parcelas")
    void shouldRenewExpiredPaidContractWhenTargetIsMetArchivingHistoryAndContinuingInstallmentNumbers(StatusContrato statusInicial) {
        String userToken = authenticateAs(Role.USER);
        LocalDate terminoAnterior = LocalDate.now().withDayOfMonth(1).minusDays(1);
        LocalDate inicioAnterior = terminoAnterior.minusMonths(2);
        LocalDate novoInicioEsperado = terminoAnterior.plusDays(1);
        LocalDate novoTerminoEsperado = novoInicioEsperado.plusMonths(3);

        long contratoId = salvarContratoReconstituido(
                statusInicial,
                inicioAnterior,
                terminoAnterior,
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                new BigDecimal("100.00"),
                List.of(
                        ParcelaDePagamento.reconstituir(1, new BigDecimal("450.00"), inicioAnterior.plusMonths(1), true, inicioAnterior.plusMonths(1)),
                        ParcelaDePagamento.reconstituir(2, new BigDecimal("450.00"), terminoAnterior, true, terminoAnterior)
                ),
                List.of()
        );

        Response response = givenAuth(userToken)
                .body(Map.of(
                        "duracaoMeses", 3,
                        "novaMeta", new BigDecimal("250.00")
                ))
                .post("/api/v1/contratos/{id}/renovacao", contratoId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        var json = response.jsonPath();
        var persistido = contratoRepository.buscarPorId(contratoId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(json.getLong("id")).isEqualTo(contratoId);
            softly.assertThat(json.getString("status")).isEqualTo("ATIVO");
            softly.assertThat(json.getString("inicio")).isEqualTo(novoInicioEsperado.toString());
            softly.assertThat(json.getString("termino")).isEqualTo(novoTerminoEsperado.toString());
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("250.00");
            softly.assertThat(json.getObject("exposicaoAcumulada", BigDecimal.class)).isEqualByComparingTo("0");
            softly.assertThat(json.getList("parcelas.numero", Integer.class)).containsExactly(3, 4, 5);
            softly.assertThat(json.getList("parcelas.paga", Boolean.class)).containsExactly(false, false, false);

            softly.assertThat(json.getList("historico")).hasSize(1);
            softly.assertThat(json.getString("historico[0].inicio")).isEqualTo(inicioAnterior.toString());
            softly.assertThat(json.getString("historico[0].termino")).isEqualTo(terminoAnterior.toString());
            softly.assertThat(json.getObject("historico[0].meta", BigDecimal.class)).isEqualByComparingTo("100.00");
            softly.assertThat(json.getObject("historico[0].exposicaoAcumulada", BigDecimal.class))
                    .isEqualByComparingTo("100.00");
            softly.assertThat(json.getList("historico[0].parcelas.numero", Integer.class)).containsExactly(1, 2);

            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.ATIVO);
            softly.assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo("0");
            softly.assertThat(persistido.getParcelas())
                    .extracting(ParcelaDePagamento::getNumero)
                    .containsExactly(3, 4, 5);
            softly.assertThat(persistido.getHistorico()).hasSize(1);
        });
    }

    @Test
    @DisplayName("POST /api/v1/contratos/{id}/renovacao deve retornar 200 com status EM_RISCO sem alterar ciclo quando meta nao foi atingida")
    void shouldMarkContractAsAtRiskWhenRenewalTargetIsNotMetKeepingCurrentCycleUnchanged() {
        String userToken = authenticateAs(Role.USER);
        LocalDate terminoAnterior = LocalDate.now().withDayOfMonth(1).minusDays(1);
        LocalDate inicioAnterior = terminoAnterior.minusMonths(2);

        long contratoId = salvarContratoReconstituido(
                StatusContrato.ATIVO,
                inicioAnterior,
                terminoAnterior,
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                new BigDecimal("99.99"),
                List.of(
                        ParcelaDePagamento.reconstituir(1, new BigDecimal("450.00"), inicioAnterior.plusMonths(1), true, inicioAnterior.plusMonths(1)),
                        ParcelaDePagamento.reconstituir(2, new BigDecimal("450.00"), terminoAnterior, true, terminoAnterior)
                ),
                List.of()
        );

        Response response = givenAuth(userToken)
                .body(Map.of(
                        "duracaoMeses", 3,
                        "novaMeta", new BigDecimal("250.00")
                ))
                .post("/api/v1/contratos/{id}/renovacao", contratoId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        var json = response.jsonPath();
        var persistido = contratoRepository.buscarPorId(contratoId).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(json.getLong("id")).isEqualTo(contratoId);
            softly.assertThat(json.getString("status")).isEqualTo("EM_RISCO");
            softly.assertThat(json.getString("inicio")).isEqualTo(inicioAnterior.toString());
            softly.assertThat(json.getString("termino")).isEqualTo(terminoAnterior.toString());
            softly.assertThat(json.getObject("meta", BigDecimal.class)).isEqualByComparingTo("100.00");
            softly.assertThat(json.getObject("exposicaoAcumulada", BigDecimal.class)).isEqualByComparingTo("99.99");
            softly.assertThat(json.getList("parcelas.numero", Integer.class)).containsExactly(1, 2);
            softly.assertThat(json.getList("parcelas.paga", Boolean.class)).containsExactly(true, true);
            softly.assertThat(json.getList("historico")).isEmpty();

            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.EM_RISCO);
            softly.assertThat(persistido.getPeriodoContratual().inicio()).isEqualTo(inicioAnterior);
            softly.assertThat(persistido.getPeriodoContratual().termino()).isEqualTo(terminoAnterior);
            softly.assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo("99.99");
            softly.assertThat(persistido.getHistorico()).isEmpty();
            softly.assertThat(persistido.getParcelas())
                    .extracting(ParcelaDePagamento::getNumero)
                    .containsExactly(1, 2);
        });
    }

    @Test
    @DisplayName("POST /api/v1/contratos/{id}/renovacao deve retornar 400 para payload invalido, JSON malformado e renovacao antecipada")
    void shouldRejectContractRenewalWithBadRequestForInvalidPayloadAndEarlyRenewal() {
        String userToken = authenticateAs(Role.USER);
        LocalDate hoje = LocalDate.now();

        long contratoTerminoHojeId = salvarContratoReconstituido(
                StatusContrato.ATIVO,
                hoje.minusMonths(2),
                hoje,
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                new BigDecimal("150.00"),
                List.of(ParcelaDePagamento.reconstituir(1, new BigDecimal("500.00"), hoje, true, hoje)),
                List.of()
        );
        long contratoTerminoFuturoId = salvarContratoReconstituido(
                StatusContrato.ATIVO,
                hoje.minusMonths(1),
                hoje.plusDays(15),
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                new BigDecimal("150.00"),
                List.of(ParcelaDePagamento.reconstituir(1, new BigDecimal("500.00"), hoje, true, hoje)),
                List.of()
        );

        Response duracaoNula = givenAuth(userToken)
                .body(Map.of("novaMeta", new BigDecimal("200.00")))
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoHojeId);
        Response duracaoZero = givenAuth(userToken)
                .body(Map.of("duracaoMeses", 0, "novaMeta", new BigDecimal("200.00")))
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoHojeId);
        Response duracaoNegativa = givenAuth(userToken)
                .body(Map.of("duracaoMeses", -2, "novaMeta", new BigDecimal("200.00")))
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoHojeId);
        Response metaNula = givenAuth(userToken)
                .body(Map.of("duracaoMeses", 3))
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoHojeId);
        Response metaZero = givenAuth(userToken)
                .body(Map.of("duracaoMeses", 3, "novaMeta", BigDecimal.ZERO))
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoHojeId);
        Response metaNegativa = givenAuth(userToken)
                .body(Map.of("duracaoMeses", 3, "novaMeta", new BigDecimal("-50.00")))
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoHojeId);
        Response jsonMalformado = givenAuth(userToken)
                .body("{\"duracaoMeses\": \"abc\", \"novaMeta\": 200.00}")
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoHojeId);
        Response antecipadaTerminoHoje = givenAuth(userToken)
                .body(Map.of("duracaoMeses", 3, "novaMeta", new BigDecimal("200.00")))
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoHojeId);
        Response antecipadaTerminoFuturo = givenAuth(userToken)
                .body(Map.of("duracaoMeses", 3, "novaMeta", new BigDecimal("200.00")))
                .post("/api/v1/contratos/{id}/renovacao", contratoTerminoFuturoId);

        assertSoftly(softly -> {
            assertApiError(softly, duracaoNula, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "duracaoMeses:");
            assertApiError(softly, duracaoZero, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "duracaoMeses:");
            assertApiError(softly, duracaoNegativa, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "duracaoMeses:");
            assertApiError(softly, metaNula, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "novaMeta:");
            assertApiError(softly, metaZero, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "novaMeta:");
            assertApiError(softly, metaNegativa, HttpStatus.BAD_REQUEST, "MethodArgumentNotValidException", "novaMeta:");
            assertApiError(
                    softly,
                    jsonMalformado,
                    HttpStatus.BAD_REQUEST,
                    "HttpMessageNotReadableException",
                    "Requisição inválida"
            );
            assertApiError(
                    softly,
                    antecipadaTerminoHoje,
                    HttpStatus.BAD_REQUEST,
                    "PeriodoInvalidoError",
                    "renovação antecipada"
            );
            assertApiError(
                    softly,
                    antecipadaTerminoFuturo,
                    HttpStatus.BAD_REQUEST,
                    "PeriodoInvalidoError",
                    "renovação antecipada"
            );
        });
    }

    @Test
    @DisplayName("POST /api/v1/contratos/{id}/renovacao deve retornar 401, 404 e 409 para nao autenticado, inexistente, pendencia financeira e status invalido")
    void shouldRejectContractRenewalForUnauthorizedNotFoundAndConflictCases() {
        String userToken = authenticateAs(Role.USER);
        LocalDate terminoAnterior = LocalDate.now().withDayOfMonth(1).minusDays(1);
        LocalDate inicioAnterior = terminoAnterior.minusMonths(2);

        long comPendenciaId = salvarContratoReconstituido(
                StatusContrato.ATIVO,
                inicioAnterior,
                terminoAnterior,
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                new BigDecimal("150.00"),
                List.of(
                        ParcelaDePagamento.reconstituir(1, new BigDecimal("450.00"), inicioAnterior.plusMonths(1), true, inicioAnterior.plusMonths(1)),
                        ParcelaDePagamento.reconstituir(2, new BigDecimal("450.00"), terminoAnterior, false, null)
                ),
                List.of()
        );
        long pendenteId = criarPropostaPendente(userToken);
        long encerradoId = salvarContratoReconstituido(
                StatusContrato.ENCERRADO,
                inicioAnterior,
                terminoAnterior,
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                new BigDecimal("150.00"),
                List.of(ParcelaDePagamento.reconstituir(1, new BigDecimal("900.00"), terminoAnterior, true, terminoAnterior)),
                List.of()
        );

        Map<String, Object> bodyValido = Map.of(
                "duracaoMeses", 3,
                "novaMeta", new BigDecimal("200.00")
        );

        Response semToken = givenAnonymous()
                .body(bodyValido)
                .post("/api/v1/contratos/{id}/renovacao", comPendenciaId);
        Response inexistente = givenAuth(userToken)
                .body(bodyValido)
                .post("/api/v1/contratos/{id}/renovacao", 999999L);
        Response pendenciaFinanceira = givenAuth(userToken)
                .body(bodyValido)
                .post("/api/v1/contratos/{id}/renovacao", comPendenciaId);
        Response contratoPendente = givenAuth(userToken)
                .body(bodyValido)
                .post("/api/v1/contratos/{id}/renovacao", pendenteId);
        Response contratoEncerrado = givenAuth(userToken)
                .body(bodyValido)
                .post("/api/v1/contratos/{id}/renovacao", encerradoId);

        assertSoftly(softly -> {
            softly.assertThat(semToken.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertApiError(
                    softly,
                    inexistente,
                    HttpStatus.NOT_FOUND,
                    "ContratoInexistenteError",
                    "Contrato não encontrado"
            );
            assertApiError(
                    softly,
                    pendenciaFinanceira,
                    HttpStatus.CONFLICT,
                    "PendenciaFinanceiraError",
                    "pendências financeiras"
            );
            assertApiError(
                    softly,
                    contratoPendente,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "ativos ou em risco"
            );
            assertApiError(
                    softly,
                    contratoEncerrado,
                    HttpStatus.CONFLICT,
                    "TransicaoDeStatusInvalidaError",
                    "ativos ou em risco"
            );
        });
    }
}











