package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
}