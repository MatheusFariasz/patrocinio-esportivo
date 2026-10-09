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
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("UnitTest")
@Tag("Functional")
class MetaContratualFunctionalTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "500"})
    @DisplayName("#19 e #54 - PE - metas equivalentes ocupam uma entrada no conjunto")
    void metasEquivalentesOcupamUmaEntrada(String valor) {
        BigDecimal numero = new BigDecimal(valor);
        MetaContratual primeira = new MetaContratual(numero);
        MetaContratual segunda = new MetaContratual(numero.setScale(4));

        assertThat(new HashSet<>(List.of(primeira, segunda))).hasSize(1);
        assertThat(primeira.valor()).isEqualByComparingTo(numero);
        assertThat(primeira).isNotEqualTo(new MetaContratual(numero.add(new BigDecimal("0.01"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "0"})
    @DisplayName("#54 - VL - rejeita meta na fronteira não positiva")
    void rejeitaMetaNaoPositiva(String valor) {
        assertThatThrownBy(() -> new MetaContratual(new BigDecimal(valor)))
                .isInstanceOf(MetaInvalidaError.class);
    }

    @Test
    @DisplayName("#28 - PE - rejeita meta ausente")
    void rejeitaMetaAusente() {
        assertThatThrownBy(() -> new MetaContratual(null))
                .isInstanceOf(MetaObrigatoriaError.class);
    }
}
