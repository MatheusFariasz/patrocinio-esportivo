package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ParcelaDePagamentoTest {

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#74 - parcelas com o mesmo valor e números diferentes devem ser entidades distintas")
    void parcelasComMesmoValorENumerosDiferentesDevemSerEntidadesDistintas() {
        ParcelaDePagamento parcela1 =
                new ParcelaDePagamento(1, new BigDecimal("1000.00"));

        ParcelaDePagamento parcela2 =
                new ParcelaDePagamento(2, new BigDecimal("1000.00"));

        assertThat(parcela1)
                .isNotEqualTo(parcela2);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#62 - deve identificar parcela vencida e não paga como em atraso")
    void deveIdentificarParcelaVencidaNaoPagaComoEmAtraso() {
        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 10, 1)
                );

        assertThat(parcela.isEmAtraso(LocalDate.of(2026, 10, 9)))
                .isTrue();
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#62 - não deve identificar parcela dentro do prazo como em atraso")
    void naoDeveIdentificarParcelaDentroDoPrazoComoEmAtraso() {
        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 10, 15)
                );

        assertThat(parcela.isEmAtraso(LocalDate.of(2026, 10, 9)))
                .isFalse();
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#62 - não deve identificar parcela paga como em atraso")
    void naoDeveIdentificarParcelaPagaComoEmAtraso() {
        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 10, 1)
                );

        parcela.registrarPagamento();

        assertThat(parcela.isEmAtraso(LocalDate.of(2026, 10, 9)))
                .isFalse();
    }
}