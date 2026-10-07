package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarContratoServiceTest {

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#9 - deve consultar contrato ativo")
    void deveConsultarContratoAtivo() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        ConsultarContratoService service =
                new ConsultarContratoService(repository);

        ContratoDePatrocinio resultado =
                service.consultar(1L);

        assertThat(resultado)
                .isSameAs(contrato);

        assertThat(resultado.getStatus())
                .isEqualTo(StatusContrato.ATIVO);
    }
}
