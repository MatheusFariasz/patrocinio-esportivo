package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.service.CancelarPropostaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;


@Tag("TDD")
class CancelarPropostaServiceTest {

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#2 - deve cancelar proposta pendente")
    void deveCancelarPropostaPendente() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(StatusContrato.PENDENTE);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(proposta));

        CancelarPropostaService service =
                new CancelarPropostaService(repository);

        service.cancelar(1L);

        assertThat(proposta.getStatus())
                .isEqualTo(StatusContrato.CANCELADO);
    }
}
