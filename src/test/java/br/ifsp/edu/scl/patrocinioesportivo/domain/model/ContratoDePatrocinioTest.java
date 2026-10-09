package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoNaoAtivoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.OperacaoRedundanteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ParcelaDuplicadaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContratoDePatrocinioTest {

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#32 - deve encerrar contrato ATIVO com multa de 20% das parcelas pendentes")
    void deveEncerrarContratoAtivoComMultaDe20PorCentoDasParcelasPendentes() {
        ParcelaDePagamento parcela1 =
                new ParcelaDePagamento(1, new BigDecimal("1000.00"));

        ParcelaDePagamento parcela2 =
                new ParcelaDePagamento(2, new BigDecimal("2000.00"));

        ParcelaDePagamento parcela3 =
                new ParcelaDePagamento(3, new BigDecimal("1500.00"));

        parcela3.registrarPagamento();

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        contrato.adicionarParcela(parcela1);
        contrato.adicionarParcela(parcela2);
        contrato.adicionarParcela(parcela3);

        contrato.encerrar();

        assertThat(contrato.getStatus())
                .isEqualTo(StatusContrato.ENCERRADO);

        assertThat(contrato.getMultaRescisoria())
                .isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#33 - não deve encerrar contrato já encerrado")
    void naoDeveEncerrarContratoJaEncerrado() {
        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ENCERRADO);

        assertThatThrownBy(contrato::encerrar)
                .isInstanceOf(OperacaoRedundanteError.class);

        assertThat(contrato.getStatus())
                .isEqualTo(StatusContrato.ENCERRADO);
    }

    @ParameterizedTest
    @EnumSource(
            value = StatusContrato.class,
            names = {"PENDENTE", "RECUSADO", "CANCELADO"}
    )
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#37 - não deve encerrar contrato com status não permitido")
    void naoDeveEncerrarContratoComStatusNaoPermitido(StatusContrato status) {
        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(status);

        assertThatThrownBy(contrato::encerrar)
                .isInstanceOf(TransicaoDeStatusInvalidaError.class);

        assertThat(contrato.getStatus())
                .isEqualTo(status);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#58 - deve encerrar contrato EM_RISCO com multa de 20% das parcelas pendentes")
    void deveEncerrarContratoEmRiscoComMultaDe20PorCentoDasParcelasPendentes() {
        ParcelaDePagamento parcela1 =
                new ParcelaDePagamento(1, new BigDecimal("1000.00"));

        ParcelaDePagamento parcela2 =
                new ParcelaDePagamento(2, new BigDecimal("2000.00"));

        ContratoDePatrocinio contrato =
                ContratoDePatrocinio.reconstituir(
                        StatusContrato.EM_RISCO,
                        null,
                        null,
                        null,
                        List.of(parcela1, parcela2)
                );

        contrato.encerrar();

        assertThat(contrato.getStatus())
                .isEqualTo(StatusContrato.ENCERRADO);

        assertThat(contrato.getMultaRescisoria())
                .isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#61 - deve calcular multa zero ao encerrar contrato ATIVO sem parcelas pendentes")
    void deveCalcularMultaZeroAoEncerrarContratoAtivoSemParcelasPendentes() {
        ParcelaDePagamento parcela1 =
                new ParcelaDePagamento(1, new BigDecimal("1000.00"));

        ParcelaDePagamento parcela2 =
                new ParcelaDePagamento(2, new BigDecimal("2000.00"));

        parcela1.registrarPagamento();
        parcela2.registrarPagamento();

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        contrato.adicionarParcela(parcela1);
        contrato.adicionarParcela(parcela2);

        contrato.encerrar();

        assertThat(contrato.getStatus())
                .isEqualTo(StatusContrato.ENCERRADO);

        assertThat(contrato.getMultaRescisoria())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#30 - deve aprovar proposta pendente, ativar o contrato e gerar as parcelas")
    void deveAprovarPropostaPendenteAtivandoOContratoEGerandoAsParcelas() {
        PeriodoContratual periodo =
                new PeriodoContratual(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1));

        MetaContratual meta =
                new MetaContratual(new BigDecimal("1000"));

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(StatusContrato.PENDENTE, periodo, meta, new BigDecimal("900.00"));

        proposta.aprovar();

        assertThat(proposta.getStatus())
                .isEqualTo(StatusContrato.ATIVO);

        assertThat(proposta.getPeriodoContratual())
                .isEqualTo(periodo);

        assertThat(proposta.getParcelas())
                .hasSize(3);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#57 - a soma das parcelas deve ser igual ao valor total, com o resto na última parcela")
    void aSomaDasParcelasDeveSerIgualAoValorTotalComORestoNaUltimaParcela() {
        PeriodoContratual periodo =
                new PeriodoContratual(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1));

        MetaContratual meta =
                new MetaContratual(new BigDecimal("1000"));

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(StatusContrato.PENDENTE, periodo, meta, new BigDecimal("100.00"));

        proposta.aprovar();

        BigDecimal soma = proposta.getParcelas().stream()
                .map(ParcelaDePagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(soma)
                .isEqualByComparingTo("100.00");

        assertThat(proposta.getParcelas().get(0).getValor())
                .isEqualByComparingTo("33.33");

        assertThat(proposta.getParcelas().get(2).getValor())
                .isEqualByComparingTo("33.34");
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#59 - as parcelas devem ser numeradas, começar não pagas e vencer dentro do período")
    void asParcelasDevemSerNumeradasComecarNaoPagasEVencerDentroDoPeriodo() {
        PeriodoContratual periodo =
                new PeriodoContratual(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1));

        MetaContratual meta =
                new MetaContratual(new BigDecimal("1000"));

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(StatusContrato.PENDENTE, periodo, meta, new BigDecimal("900.00"));

        proposta.aprovar();

        assertThat(proposta.getParcelas())
                .extracting(ParcelaDePagamento::getNumero)
                .containsExactly(1, 2, 3);

        assertThat(proposta.getParcelas())
                .noneMatch(ParcelaDePagamento::isPaga);

        assertThat(proposta.getParcelas())
                .extracting(ParcelaDePagamento::getVencimento)
                .allSatisfy(vencimento -> assertThat(vencimento)
                        .isBetween(periodo.inicio(), periodo.termino()));
    }

    @ParameterizedTest
    @EnumSource(
            value = StatusContrato.class,
            names = {"ATIVO", "EM_RISCO", "ENCERRADO", "RECUSADO", "CANCELADO"}
    )
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#31 - não deve aprovar proposta que não está pendente")
    void naoDeveAprovarPropostaQueNaoEstaPendente(StatusContrato status) {
        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(status);

        assertThatThrownBy(contrato::aprovar)
                .isInstanceOf(TransicaoDeStatusInvalidaError.class);

        assertThat(contrato.getStatus())
                .isEqualTo(status);

        assertThat(contrato.getParcelas())
                .isEmpty();
    }

    @ParameterizedTest
    @EnumSource(
            value = StatusContrato.class,
            names = {"PENDENTE", "EM_RISCO", "ENCERRADO", "RECUSADO", "CANCELADO"}
    )
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#73 - não deve adicionar parcela a contrato que não está ATIVO")
    void naoDeveAdicionarParcelaAContratoQueNaoEstaAtivo(StatusContrato status) {
        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(status);

        ParcelaDePagamento parcela =
                new ParcelaDePagamento(1, new BigDecimal("1000.00"));

        assertThatThrownBy(() -> contrato.adicionarParcela(parcela))
                .isInstanceOf(ContratoNaoAtivoError.class);

        assertThat(contrato.getParcelas())
                .isEmpty();
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#75 - não deve adicionar parcela com identificador já existente no contrato")
    void naoDeveAdicionarParcelaComIdentificadorDuplicado() {
        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        contrato.adicionarParcela(
                new ParcelaDePagamento(1, new BigDecimal("1000.00"))
        );

        ParcelaDePagamento parcelaDuplicada =
                new ParcelaDePagamento(1, new BigDecimal("500.00"));

        assertThatThrownBy(() -> contrato.adicionarParcela(parcelaDuplicada))
                .isInstanceOf(ParcelaDuplicadaError.class);

        assertThat(contrato.getParcelas())
                .hasSize(1);
    }
}