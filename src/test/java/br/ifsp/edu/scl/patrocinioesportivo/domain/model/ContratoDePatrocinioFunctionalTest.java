package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
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

@Tag("UnitTest")
@Tag("Functional")
class ContratoDePatrocinioFunctionalTest {
    private ContratoDePatrocinio contrato(StatusContrato status, List<ParcelaDePagamento> parcelas) {
        LocalDate inicio = LocalDate.of(2026, 10, 10);
        return ContratoDePatrocinio.reconstituir(status,
                new PeriodoContratual(inicio, inicio.plusMonths(3)),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000"), parcelas);
    }

    @ParameterizedTest
    @EnumSource(StatusContrato.class)
    @DisplayName("#93 - PE - getter não permite inserir parcelas")
    void naoPermiteInserirPeloGetter(StatusContrato status) {
        ContratoDePatrocinio contrato = contrato(status, List.of());
        ParcelaDePagamento parcela = new ParcelaDePagamento(1, BigDecimal.TEN);

        assertThatThrownBy(() -> contrato.getParcelas().add(parcela))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(contrato.getParcelas()).isEmpty();
    }

    @Test
    @DisplayName("#93 - PE - getter não permite inserir número duplicado")
    void naoPermiteInserirNumeroDuplicadoPeloGetter() {
        ParcelaDePagamento original = new ParcelaDePagamento(1, BigDecimal.TEN);
        ContratoDePatrocinio contrato = contrato(StatusContrato.ATIVO, List.of(original));

        assertThatThrownBy(() -> contrato.getParcelas().add(
                new ParcelaDePagamento(1, BigDecimal.ONE)))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(contrato.getParcelas()).containsExactly(original);
    }

    @Test
    @DisplayName("#93 - PE - getter não permite remover obrigações")
    void naoPermiteRemoverPeloGetter() {
        ParcelaDePagamento original = new ParcelaDePagamento(1, BigDecimal.TEN);
        ContratoDePatrocinio contrato = contrato(StatusContrato.ATIVO, List.of(original));

        assertThatThrownBy(() -> contrato.getParcelas().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(contrato.getParcelas()).containsExactly(original);
    }
    @Test
    @DisplayName("#93 - PE - getter não permite substituir uma obrigação")
    void naoPermiteSubstituirPeloGetter() {
        ParcelaDePagamento original = new ParcelaDePagamento(1, BigDecimal.TEN);
        ContratoDePatrocinio contrato = contrato(StatusContrato.ATIVO, List.of(original));

        assertThatThrownBy(() -> contrato.getParcelas().set(
                0, new ParcelaDePagamento(2, BigDecimal.ONE)))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(contrato.getParcelas()).containsExactly(original);
    }
}
