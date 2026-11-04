
package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RegistrarExposicaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("Functional")
class RegistrarExposicaoServiceFunctionalTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private RegistrarExposicaoService service;

    @Test
    @DisplayName("#14 - PE - soma e salva a exposição registrada")
    void somaESalvaExposicao() {
        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        contrato.definirId(1L);
        contrato.registrarExposicao(new BigDecimal("50"));

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        service.registrar(1L, new BigDecimal("100"));

        assertThat(contrato.getExposicaoAcumulada())
                .isEqualByComparingTo("150");

        verify(repository).salvar(contrato);
    }
}
