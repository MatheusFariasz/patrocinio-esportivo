package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.HistoricoDePeriodo;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc.JdbcContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.support.ContratoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Testes de persistência JdbcContratoDePatrocinioRepository")
class JdbcContratoDePatrocinioRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcContratoDePatrocinioRepository repository;

    @BeforeEach
    void setUp() {
        var dataSource = ContratoTestFixtures.createInitializedSqliteDataSource();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.repository = new JdbcContratoDePatrocinioRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("Deve salvar novo contrato e recupera-lo por ID preservando decimais exatos e atributos")
    void shouldSaveNewContratoAndFindByIdPreservingExactDecimalsAndAttributes() {
        var valorExato = new BigDecimal("98765432109876.123456789");
        var metaExata = new BigDecimal("1500.755");
        var contrato = ContratoTestFixtures.contratoPendente(valorExato, metaExata);

        var salvo = repository.salvar(contrato);

        assertThat(salvo.getId()).isNotNull().isPositive();
        var persistido = repository.buscarPorId(salvo.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(persistido.getId()).isEqualTo(salvo.getId());
            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.PENDENTE);
            softly.assertThat(persistido.getClubeId()).isEqualTo(ContratoTestFixtures.CLUBE_DEMO_ID);
            softly.assertThat(persistido.getPatrocinadorId()).isEqualTo(ContratoTestFixtures.PATROCINADOR_DEMO_ID);
            softly.assertThat(persistido.getPeriodoContratual().inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
            softly.assertThat(persistido.getPeriodoContratual().termino()).isEqualTo(LocalDate.of(2026, 4, 1));
            softly.assertThat(persistido.getValorTotal()).isEqualTo(valorExato);
            softly.assertThat(persistido.getMetaContratual().valor()).isEqualTo(metaExata);
            softly.assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo(BigDecimal.ZERO);
            softly.assertThat(persistido.getMultaRescisoria()).isEqualByComparingTo(BigDecimal.ZERO);
            softly.assertThat(persistido.getParcelas()).isEmpty();
            softly.assertThat(persistido.getHistorico()).isEmpty();
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1L, 0L, 999999L})
    @DisplayName("Deve retornar vazio ao buscar contrato por ID inexistente ou nulo")
    void shouldReturnEmptyWhenSearchingNonExistentContratoId(Long id) {
        assertThat(repository.buscarPorId(id)).isEmpty();
    }

    @Test
    @DisplayName("Deve atualizar contrato existente substituindo atributos, parcelas e historico sem duplicacao")
    void shouldUpdateExistingContratoReplacingParcelasAndHistorico() {
        var inicial = repository.salvar(ContratoTestFixtures.contratoPendente(
                new BigDecimal("1000.00"), new BigDecimal("100.00")
        ));
        Long id = inicial.getId();

        var parcelaHistorico = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("1000.00"), LocalDate.of(2026, 2, 1), true, LocalDate.of(2026, 1, 30)
        );
        var historico = new HistoricoDePeriodo(
                new PeriodoContratual(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1)),
                new MetaContratual(new BigDecimal("100.00")),
                new BigDecimal("120.00"),
                List.of(parcelaHistorico)
        );
        var novaParcela = ParcelaDePagamento.reconstituir(
                2, new BigDecimal("2500.50"), LocalDate.of(2026, 6, 1), false, null
        );
        var atualizado = ContratoDePatrocinio.reconstituir(
                id,
                StatusContrato.ENCERRADO,
                new PeriodoContratual(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 7, 2)),
                new MetaContratual(new BigDecimal("250.00")),
                new BigDecimal("2500.50"),
                ContratoTestFixtures.CLUBE_DEMO_ID,
                ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                new BigDecimal("80.25"),
                new BigDecimal("500.10"),
                List.of(novaParcela),
                List.of(historico)
        );

        repository.salvar(atualizado);
        repository.salvar(atualizado);

        var persistido = repository.buscarPorId(id).orElseThrow();
        assertThat(persistido.getParcelas()).hasSize(1);
        assertThat(persistido.getHistorico()).hasSize(1);
        var cicloSalvo = persistido.getHistorico().getFirst();
        assertThat(cicloSalvo.parcelas()).hasSize(1);

        assertSoftly(softly -> {
            softly.assertThat(persistido.getStatus()).isEqualTo(StatusContrato.ENCERRADO);
            softly.assertThat(persistido.getPeriodoContratual().inicio()).isEqualTo(LocalDate.of(2026, 4, 2));
            softly.assertThat(persistido.getPeriodoContratual().termino()).isEqualTo(LocalDate.of(2026, 7, 2));
            softly.assertThat(persistido.getMetaContratual().valor()).isEqualByComparingTo("250.00");
            softly.assertThat(persistido.getValorTotal()).isEqualByComparingTo("2500.50");
            softly.assertThat(persistido.getExposicaoAcumulada()).isEqualByComparingTo("80.25");
            softly.assertThat(persistido.getMultaRescisoria()).isEqualByComparingTo("500.10");

            softly.assertThat(persistido.getParcelas().getFirst().getNumero()).isEqualTo(2);
            softly.assertThat(persistido.getParcelas().getFirst().isPaga()).isFalse();
            softly.assertThat(persistido.getParcelas().getFirst().getDataPagamento()).isNull();

            softly.assertThat(cicloSalvo.exposicaoAcumulada()).isEqualByComparingTo("120.00");
            softly.assertThat(cicloSalvo.parcelas().getFirst().getNumero()).isEqualTo(1);
            softly.assertThat(cicloSalvo.parcelas().getFirst().isPaga()).isTrue();
            softly.assertThat(cicloSalvo.parcelas().getFirst().getDataPagamento()).isEqualTo(LocalDate.of(2026, 1, 30));
        });
    }

    @Test
    @DisplayName("Deve lancar ContratoInexistenteError ao tentar atualizar contrato com ID inexistente")
    void shouldThrowContratoInexistenteErrorWhenUpdatingNonExistentId() {
        var parcela = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("500.00"), LocalDate.of(2026, 6, 1), false, null
        );
        var contratoInexistente = ContratoTestFixtures.contratoAtivoComParcelas(
                9999L, List.of(parcela), List.of()
        );

        assertThatThrownBy(() -> repository.salvar(contratoInexistente))
                .isInstanceOf(ContratoInexistenteError.class)
                .hasMessage("Contrato não encontrado.");

        assertSoftly(softly -> {
            softly.assertThat(repository.buscarPorId(9999L)).isEmpty();
            softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contrato_patrocinio", Integer.class))
                    .isZero();
            softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM parcela_pagamento", Integer.class))
                    .isZero();
            softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM historico_periodo", Integer.class))
                    .isZero();
        });
    }

    @Test
    @DisplayName("Deve retornar parcelas ordenadas por numero e historico ordenado por ciclo mesmo inseridos fora de ordem")
    void shouldReturnParcelasOrderedByNumeroAndHistoricoOrderedByCiclo() {
        var salvo = repository.salvar(ContratoTestFixtures.contratoAtivoComParcelas(null, List.of(), List.of()));
        Long contratoId = salvo.getId();

        jdbcTemplate.update(
                "INSERT INTO historico_periodo (contrato_id, ciclo, inicio, termino, meta, exposicao_acumulada) VALUES (?, ?, ?, ?, ?, ?)",
                contratoId, 2, "2025-07-01", "2025-12-31", "200.00", "210.00"
        );
        jdbcTemplate.update(
                "INSERT INTO historico_periodo (contrato_id, ciclo, inicio, termino, meta, exposicao_acumulada) VALUES (?, ?, ?, ?, ?, ?)",
                contratoId, 1, "2025-01-01", "2025-06-30", "100.00", "110.00"
        );

        jdbcTemplate.update(
                "INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?)",
                contratoId, 0, 12, "400.00", null, 0, null
        );
        jdbcTemplate.update(
                "INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?)",
                contratoId, 0, 10, "400.00", "2026-06-01", 1, "2026-05-28"
        );
        jdbcTemplate.update(
                "INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?)",
                contratoId, 0, 11, "400.00", "2026-07-01", 0, null
        );

        jdbcTemplate.update(
                "INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?)",
                contratoId, 1, 3, "300.00", "2025-03-01", 1, "2025-03-01"
        );
        jdbcTemplate.update(
                "INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?)",
                contratoId, 1, 1, "300.00", "2025-01-31", 1, "2025-01-30"
        );
        jdbcTemplate.update(
                "INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?)",
                contratoId, 2, 6, "350.00", "2025-09-01", 1, "2025-09-01"
        );
        jdbcTemplate.update(
                "INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?)",
                contratoId, 2, 4, "350.00", "2025-07-31", 1, "2025-07-30"
        );

        var persistido = repository.buscarPorId(contratoId).orElseThrow();
        assertThat(persistido.getHistorico()).hasSize(2);

        assertSoftly(softly -> {
            softly.assertThat(persistido.getParcelas())
                    .extracting(ParcelaDePagamento::getNumero)
                    .containsExactly(10, 11, 12);
            softly.assertThat(persistido.getParcelas().getLast().getVencimento()).isNull();
            softly.assertThat(persistido.getParcelas().getLast().getDataPagamento()).isNull();

            softly.assertThat(persistido.getHistorico())
                    .extracting(h -> h.periodo().inicio())
                    .containsExactly(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 1));
            softly.assertThat(persistido.getHistorico().get(0).parcelas())
                    .extracting(ParcelaDePagamento::getNumero)
                    .containsExactly(1, 3);
            softly.assertThat(persistido.getHistorico().get(1).parcelas())
                    .extracting(ParcelaDePagamento::getNumero)
                    .containsExactly(4, 6);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROVADO", "INVALIDO", "ativo", ""})
    @DisplayName("Deve rejeitar status fora do dominio pela constraint CHECK de contrato_patrocinio")
    void shouldRejectInvalidStatusByCheckConstraint(String statusInvalido) {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO contrato_patrocinio
                (clube_id, patrocinador_id, status, inicio, termino, meta, valor_total, exposicao_acumulada, multa_rescisoria)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                ContratoTestFixtures.CLUBE_DEMO_ID,
                ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                statusInvalido,
                "2026-01-01",
                "2026-04-01",
                "100.00",
                "1000.00",
                "0",
                "0"
        )).isInstanceOf(DataAccessException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contrato_patrocinio", Integer.class))
                .isZero();
    }

    @ParameterizedTest(name = "parcela invalida: ciclo={0}, numero={1}, paga={2}")
    @CsvSource({
            "-1, 1, 0",
            "0, 0, 0",
            "0, -3, 1",
            "0, 1, -1",
            "0, 1, 2"
    })
    @DisplayName("Deve rejeitar valores invalidos pelas constraints CHECK de parcela_pagamento e historico_periodo")
    void shouldRejectInvalidCheckValuesInParcelaAndHistorico(int ciclo, int numero, int paga) {
        Long contratoId = repository.salvar(ContratoTestFixtures.contratoAtivoComParcelas(null, List.of(), List.of()))
                .getId();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    contratoId, ciclo, numero, "500.00", "2026-06-01", paga, null
            )).isInstanceOf(DataAccessException.class);

            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO historico_periodo (contrato_id, ciclo, inicio, termino, meta, exposicao_acumulada)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    contratoId, 0, "2025-01-01", "2025-06-30", "100.00", "50.00"
            )).isInstanceOf(DataAccessException.class);

            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO historico_periodo (contrato_id, ciclo, inicio, termino, meta, exposicao_acumulada)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    contratoId, -1, "2025-01-01", "2025-06-30", "100.00", "50.00"
            )).isInstanceOf(DataAccessException.class);

            softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM parcela_pagamento", Integer.class))
                    .isZero();
            softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM historico_periodo", Integer.class))
                    .isZero();
        });
    }

    @Test
    @DisplayName("Deve rejeitar parcela duplicada e historico duplicado pelas chaves primarias compostas")
    void shouldRejectDuplicateParcelaAndDuplicateHistoricoByCompositePrimaryKey() {
        var parcela1 = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("500.00"), LocalDate.of(2026, 6, 1), false, null
        );
        var historico1 = new HistoricoDePeriodo(
                new PeriodoContratual(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30)),
                new MetaContratual(new BigDecimal("100.00")),
                new BigDecimal("120.00"),
                List.of()
        );
        Long contratoId = repository.salvar(ContratoTestFixtures.contratoAtivoComParcelas(
                null, List.of(parcela1), List.of(historico1)
        )).getId();

        var contratoComParcelasDuplicadas = ContratoTestFixtures.contratoAtivoComParcelas(
                contratoId,
                List.of(
                        ParcelaDePagamento.reconstituir(2, new BigDecimal("250.00"), LocalDate.of(2026, 6, 1), false, null),
                        ParcelaDePagamento.reconstituir(2, new BigDecimal("250.00"), LocalDate.of(2026, 7, 1), false, null)
                ),
                List.of()
        );

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    contratoId, 0, 1, "999.00", "2026-07-01", 0, null
            )).isInstanceOf(DataAccessException.class);

            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO historico_periodo (contrato_id, ciclo, inicio, termino, meta, exposicao_acumulada)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    contratoId, 1, "2025-07-01", "2025-12-31", "200.00", "150.00"
            )).isInstanceOf(DataAccessException.class);

            softly.assertThatThrownBy(() -> repository.salvar(contratoComParcelasDuplicadas))
                    .isInstanceOf(DataAccessException.class);
        });
    }

    @Test
    @DisplayName("Deve rejeitar chaves estrangeiras inexistentes e colunas obrigatorias nulas")
    void shouldRejectInvalidForeignKeysAndNullMandatoryColumns() {
        var periodo = new PeriodoContratual(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1));
        var meta = new MetaContratual(new BigDecimal("100.00"));
        var clubeInexistente = new ContratoDePatrocinio(
                StatusContrato.PENDENTE, periodo, meta, new BigDecimal("1000.00"),
                999L, ContratoTestFixtures.PATROCINADOR_DEMO_ID
        );
        var patrocinadorInexistente = new ContratoDePatrocinio(
                StatusContrato.PENDENTE, periodo, meta, new BigDecimal("1000.00"),
                ContratoTestFixtures.CLUBE_DEMO_ID, 888L
        );

        Long contratoValidoId = repository.salvar(ContratoTestFixtures.contratoPendente(
                new BigDecimal("1000.00"), new BigDecimal("100.00")
        )).getId();

        assertSoftly(softly -> {
            softly.assertThat(jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class))
                    .isEqualTo(1);

            softly.assertThatThrownBy(() -> repository.salvar(clubeInexistente))
                    .isInstanceOf(DataAccessException.class);
            softly.assertThat(clubeInexistente.getId()).isNull();

            softly.assertThatThrownBy(() -> repository.salvar(patrocinadorInexistente))
                    .isInstanceOf(DataAccessException.class);
            softly.assertThat(patrocinadorInexistente.getId()).isNull();

            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    99999L, 0, 1, "500.00", "2026-06-01", 0, null
            )).isInstanceOf(DataAccessException.class);

            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO historico_periodo (contrato_id, ciclo, inicio, termino, meta, exposicao_acumulada)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    99999L, 1, "2025-01-01", "2025-06-30", "100.00", "80.00"
            )).isInstanceOf(DataAccessException.class);

            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO contrato_patrocinio
                    (clube_id, patrocinador_id, status, inicio, termino, meta, valor_total, exposicao_acumulada, multa_rescisoria)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    ContratoTestFixtures.CLUBE_DEMO_ID, ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                    "PENDENTE", "2026-01-01", "2026-04-01", null, "1000.00", "0", "0"
            )).isInstanceOf(DataAccessException.class);

            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    contratoValidoId, 0, 1, null, "2026-06-01", 0, null
            )).isInstanceOf(DataAccessException.class);

            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO historico_periodo (contrato_id, ciclo, inicio, termino, meta, exposicao_acumulada)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    contratoValidoId, 1, null, "2025-06-30", "100.00", "80.00"
            )).isInstanceOf(DataAccessException.class);

            softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contrato_patrocinio", Integer.class))
                    .isEqualTo(1);
            softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM parcela_pagamento", Integer.class))
                    .isZero();
            softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM historico_periodo", Integer.class))
                    .isZero();
        });
    }

    @Test
    @DisplayName("Deve remover parcelas e historico em cascata ao deletar contrato sem afetar outros contratos")
    void shouldDeleteParcelasAndHistoricoOnCascadeWhenContratoIsDeleted() {
        var parcelaAtualAlvo = ParcelaDePagamento.reconstituir(
                2, new BigDecimal("600.00"), LocalDate.of(2026, 6, 1), false, null
        );
        var parcelaHistoricoAlvo = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("600.00"), LocalDate.of(2025, 6, 1), true, LocalDate.of(2025, 5, 30)
        );
        var historicoAlvo = new HistoricoDePeriodo(
                new PeriodoContratual(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30)),
                new MetaContratual(new BigDecimal("100.00")),
                new BigDecimal("110.00"),
                List.of(parcelaHistoricoAlvo)
        );
        Long alvoId = repository.salvar(ContratoTestFixtures.contratoAtivoComParcelas(
                null, List.of(parcelaAtualAlvo), List.of(historicoAlvo)
        )).getId();

        var parcelaOutro = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("800.00"), LocalDate.of(2026, 7, 1), false, null
        );
        var historicoOutro = new HistoricoDePeriodo(
                new PeriodoContratual(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30)),
                new MetaContratual(new BigDecimal("150.00")),
                new BigDecimal("160.00"),
                List.of()
        );
        Long outroId = repository.salvar(ContratoTestFixtures.contratoAtivoComParcelas(
                null, List.of(parcelaOutro), List.of(historicoOutro)
        )).getId();

        int deletados = jdbcTemplate.update("DELETE FROM contrato_patrocinio WHERE id = ?", alvoId);

        assertSoftly(softly -> {
            softly.assertThat(deletados).isEqualTo(1);
            softly.assertThat(repository.buscarPorId(alvoId)).isEmpty();
            softly.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM parcela_pagamento WHERE contrato_id = ?", Integer.class, alvoId
            )).isZero();
            softly.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM historico_periodo WHERE contrato_id = ?", Integer.class, alvoId
            )).isZero();

            softly.assertThat(repository.buscarPorId(outroId)).isPresent();
            softly.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM parcela_pagamento WHERE contrato_id = ?", Integer.class, outroId
            )).isEqualTo(1);
            softly.assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM historico_periodo WHERE contrato_id = ?", Integer.class, outroId
            )).isEqualTo(1);
        });
    }
}
