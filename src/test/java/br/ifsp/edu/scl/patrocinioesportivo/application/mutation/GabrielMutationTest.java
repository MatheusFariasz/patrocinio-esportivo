package br.ifsp.edu.scl.patrocinioesportivo.application.mutation;

import br.ifsp.edu.scl.patrocinioesportivo.model.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.*;
import br.ifsp.edu.scl.patrocinioesportivo.service.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


import static br.ifsp.edu.scl.patrocinioesportivo.application.mutation.DadosMutacao.*;
import static org.assertj.core.api.Assertions.*;

@Tag("UnitTest")
@Tag("Mutation")
class GabrielMutationTest {

    @Test
    void HashDaParcelaDeveSerCalculadoPeloNumero() {
        var primeira = new ParcelaDePagamento(7, valor("100"));
        var mesmaIdentidade = new ParcelaDePagamento(7, valor("200"));
        assertThat(primeira).isEqualTo(mesmaIdentidade);
        assertThat(primeira.hashCode()).isEqualTo(Integer.hashCode(7));
        assertThat(primeira.hashCode()).isEqualTo(mesmaIdentidade.hashCode());
    }
}