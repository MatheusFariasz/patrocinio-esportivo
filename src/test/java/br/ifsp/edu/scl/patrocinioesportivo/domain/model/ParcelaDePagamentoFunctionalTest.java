package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UnitTest")
@Tag("Functional")
class ParcelaDePagamentoFunctionalTest {

    @ParameterizedTest(name = "vencimento {0} dia(s), paga={1}, atraso={2}")
    @CsvSource({
            "-1, false, true", "0, false, false", "1, false, false",
            "-1, true, false", "0, true, false", "1, true, false"
    })
    @DisplayName("#62 - VL/TD - cruza vencimento e quitação na avaliação de atraso")
    void avaliaAtrasoNaFronteiraDoVencimento(int dias, boolean paga, boolean atraso) {
        LocalDate hoje = LocalDate.of(2026, 10, 10);
        ParcelaDePagamento parcela = new ParcelaDePagamento(
                1, new BigDecimal("1000"), hoje.plusDays(dias));
        if (paga) {
            parcela.registrarPagamento();
        }

        assertThat(parcela.isEmAtraso(hoje)).isEqualTo(atraso);
        assertThat(parcela.isPaga()).isEqualTo(paga);
        assertThat(parcela.getVencimento()).isEqualTo(hoje.plusDays(dias));
    }
}
