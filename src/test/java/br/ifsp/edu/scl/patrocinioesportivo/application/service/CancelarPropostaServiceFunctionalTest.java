package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.CancelarPropostaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("Functional")
class CancelarPropostaServiceFunctionalTest {
    @Mock private ContratoDePatrocinioRepository repository;
    @InjectMocks private CancelarPropostaService service;

    @Test
    @DisplayName("BUG-03 - PE - salva o resultado do cancelamento")
    void salvaCancelamento() {
        ContratoDePatrocinio proposta = new ContratoDePatrocinio(StatusContrato.PENDENTE);
        proposta.definirId(1L);
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(proposta));

        service.cancelar(1L);

        assertThat(proposta.getStatus()).isEqualTo(StatusContrato.CANCELADO);
        verify(repository).salvar(proposta);
    }
}
