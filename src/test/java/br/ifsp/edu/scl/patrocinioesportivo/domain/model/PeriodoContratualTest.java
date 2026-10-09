package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("UnitTest")
@Tag("TDD")
class PeriodoContratualTest {

    @Test
    @DisplayName("#71 - deve criar novo período ao alterar início sem modificar o original")
    void deveCriarNovoPeriodoAoAlterarInicio() {
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate termino = inicio.plusMonths(3);
        PeriodoContratual original = new PeriodoContratual(inicio, termino);

        PeriodoContratual atualizado = original.comInicio(inicio.plusDays(1));

        assertThat(atualizado).isNotSameAs(original);
        assertThat(atualizado.inicio()).isEqualTo(inicio.plusDays(1));
        assertThat(atualizado.termino()).isEqualTo(termino);
        assertThat(original.inicio()).isEqualTo(inicio);
        assertThat(original.termino()).isEqualTo(termino);
    }

    @Test
    @DisplayName("#72 - deve criar novo período ao alterar término sem modificar o original")
    void deveCriarNovoPeriodoAoAlterarTermino() {
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate termino = inicio.plusMonths(3);
        PeriodoContratual original = new PeriodoContratual(inicio, termino);

        PeriodoContratual atualizado = original.comTermino(termino.plusMonths(1));

        assertThat(atualizado).isNotSameAs(original);
        assertThat(atualizado.inicio()).isEqualTo(inicio);
        assertThat(atualizado.termino()).isEqualTo(termino.plusMonths(1));
        assertThat(original.inicio()).isEqualTo(inicio);
        assertThat(original.termino()).isEqualTo(termino);
    }

    @Test
    @DisplayName("#89 - períodos com as mesmas datas devem ser iguais por valor")
    void deveCompararPeriodosPorValor() {
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate termino = inicio.plusMonths(3);
        PeriodoContratual primeiro = new PeriodoContratual(inicio, termino);
        PeriodoContratual segundo = new PeriodoContratual(inicio, termino);

        assertThat(primeiro).isNotSameAs(segundo).isEqualTo(segundo);
        assertThat(primeiro.hashCode()).isEqualTo(segundo.hashCode());
    }

    @Test
    @DisplayName("#19 - deve representar um período histórico com datas válidas")
    void deveRepresentarPeriodoHistorico() {
        LocalDate inicio = LocalDate.now().minusMonths(3);
        LocalDate termino = LocalDate.now().minusDays(1);

        PeriodoContratual periodo = new PeriodoContratual(inicio, termino);

        assertThat(periodo.inicio()).isEqualTo(inicio);
        assertThat(periodo.termino()).isEqualTo(termino);
    }

    @ParameterizedTest
    @MethodSource("periodosInvalidos")
    @DisplayName("#27 e #53 - não deve criar um período contratual inválido")
    void naoDeveCriarPeriodoInvalido(LocalDate inicio, LocalDate termino) {
        assertThatThrownBy(() -> new PeriodoContratual(inicio, termino))
                .isInstanceOf(PeriodoInvalidoError.class);
    }

    static Stream<Arguments> periodosInvalidos() {
        LocalDate hoje = LocalDate.now();
        return Stream.of(
                Arguments.of(null, hoje.plusMonths(3)),
                Arguments.of(hoje.plusDays(1), null),
                Arguments.of(hoje.plusDays(1), hoje.plusDays(1)),
                Arguments.of(hoje.plusDays(10), hoje.plusDays(1))
        );
    }
}
