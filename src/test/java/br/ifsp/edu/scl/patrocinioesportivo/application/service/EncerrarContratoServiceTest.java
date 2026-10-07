package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.EncerrarContratoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncerrarContratoServiceTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private EncerrarContratoService service;

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#35 - deve lançar erro ao tentar encerrar contrato inexistente")
    void deveLancarErroAoTentarEncerrarContratoInexistente() {
        Long contratoId = (Long) 1L;

        when(repository.buscarPorId(contratoId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.encerrar(contratoId))
                .isInstanceOf(ContratoInexistenteError.class);
    }
}