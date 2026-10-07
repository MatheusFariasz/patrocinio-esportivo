package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.OperacaoRedundanteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

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
                new ContratoDePatrocinio(StatusContrato.EM_RISCO);

        contrato.adicionarParcela(parcela1);
        contrato.adicionarParcela(parcela2);

        contrato.encerrar();

        assertThat(contrato.getStatus())
                .isEqualTo(StatusContrato.ENCERRADO);

        assertThat(contrato.getMultaRescisoria())
                .isEqualByComparingTo(new BigDecimal("600.00"));
    }
}