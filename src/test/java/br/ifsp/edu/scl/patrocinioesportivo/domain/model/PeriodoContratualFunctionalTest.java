package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("UnitTest")
@Tag("Functional")
class PeriodoContratualFunctionalTest {
    @Test
    @DisplayName("#27 e #53 - MF-02 - VL - aceita término um dia após o início")
    void aceitaPeriodoDeUmDia() {
        LocalDate inicio = LocalDate.of(2026, 10, 10);

        PeriodoContratual periodo = new PeriodoContratual(inicio, inicio.plusDays(1));

        assertThat(periodo.inicio()).isEqualTo(inicio);
        assertThat(periodo.termino()).isEqualTo(inicio.plusDays(1));
    }

    @ParameterizedTest(name = "alteração de {0} dia(s)")
    @ValueSource(ints = {1, 30})
    @DisplayName("#71, #72 e #89 - MF-04 - PE - conserva igualdade e original após criar períodos")
    void conservaOriginalEIgualdadeAoCriarPeriodos(int dias) {
        LocalDate inicio = LocalDate.of(2026, 10, 10);
        LocalDate termino = inicio.plusMonths(3);
        PeriodoContratual original = new PeriodoContratual(inicio, termino);
        PeriodoContratual equivalente = new PeriodoContratual(inicio, termino);

        PeriodoContratual novoInicio = original.comInicio(inicio.plusDays(dias));
        PeriodoContratual novoTermino = original.comTermino(termino.plusDays(dias));

        assertThat(new HashSet<>(List.of(original, equivalente))).hasSize(1);
        assertThat(original).isEqualTo(equivalente);
        assertThat(novoInicio).isNotEqualTo(original);
        assertThat(novoTermino).isNotEqualTo(original);
        assertThat(novoInicio.inicio()).isEqualTo(inicio.plusDays(dias));
        assertThat(novoInicio.termino()).isEqualTo(termino);
        assertThat(novoTermino.inicio()).isEqualTo(inicio);
        assertThat(novoTermino.termino()).isEqualTo(termino.plusDays(dias));
        assertThat(original.inicio()).isEqualTo(inicio);
        assertThat(original.termino()).isEqualTo(termino);
    }
}
