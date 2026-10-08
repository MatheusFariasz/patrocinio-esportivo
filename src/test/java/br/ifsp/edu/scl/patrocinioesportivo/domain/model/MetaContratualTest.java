package br.ifsp.edu.scl.patrocinioesportivo.domain.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("UnitTest")
@Tag("TDD")
class MetaContratualTest {

    @Test
    @DisplayName("#19 - deve comparar metas pelo valor independentemente das casas decimais")
    void deveCompararMetasPorValor() {
        MetaContratual primeira = new MetaContratual(new BigDecimal("500"));
        MetaContratual segunda = new MetaContratual(new BigDecimal("500.00"));

        assertThat(primeira.valor()).isEqualByComparingTo("500");
        assertThat(primeira).isEqualTo(segunda);
        assertThat(primeira.hashCode()).isEqualTo(segunda.hashCode());
    }

    @Test
    @DisplayName("#28 - não deve criar meta contratual sem valor")
    void naoDeveCriarMetaSemValor() {
        assertThatThrownBy(() -> new MetaContratual(null))
                .isInstanceOf(MetaObrigatoriaError.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-500"})
    @DisplayName("#54 - não deve criar meta contratual menor ou igual a zero")
    void naoDeveCriarMetaInvalida(BigDecimal valor) {
        assertThatThrownBy(() -> new MetaContratual(valor))
                .isInstanceOf(MetaInvalidaError.class);
    }
}
