package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PermissaoNegadaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.AprovarPropostaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AprovarPropostaServiceTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private AprovarPropostaService service;

    @ParameterizedTest
    @EnumSource(
            value = PerfilUsuario.class,
            names = "DIRETOR_FINANCEIRO",
            mode = EnumSource.Mode.EXCLUDE
    )
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#34 - deve lançar erro quando quem aprova não é diretor financeiro")
    void deveLancarErroQuandoQuemAprovaNaoEDiretorFinanceiro(PerfilUsuario perfil) {
        assertThatThrownBy(() -> service.aprovar(perfil, 1L))
                .isInstanceOf(PermissaoNegadaError.class);

        verifyNoInteractions(repository);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#36 - deve lançar erro ao tentar aprovar proposta inexistente")
    void deveLancarErroAoTentarAprovarPropostaInexistente() {
        Long contratoId = 1L;

        when(repository.buscarPorId(contratoId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprovar(PerfilUsuario.DIRETOR_FINANCEIRO, contratoId))
                .isInstanceOf(ContratoInexistenteError.class);
    }
}