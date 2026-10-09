package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RecusarPropostaService;
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
class RecusarPropostaServiceTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private RecusarPropostaService service;

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#51 - deve lançar erro ao tentar recusar proposta inexistente")
    void deveLancarErroAoTentarRecusarPropostaInexistente() {
        Long contratoId = 1L;

        when(repository.buscarPorId(contratoId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recusar(PerfilUsuario.DIRETOR_FINANCEIRO, contratoId))
                .isInstanceOf(ContratoInexistenteError.class);
    }
}