package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PermissaoNegadaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#30 - deve aprovar a proposta pendente e salvá-la")
    void deveAprovarAPropostaPendenteESalva() {
        PeriodoContratual periodo =
                new PeriodoContratual(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1));

        MetaContratual meta =
                new MetaContratual(new BigDecimal("1000"));

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(StatusContrato.PENDENTE, periodo, meta, new BigDecimal("900.00"));

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(proposta));

        service.aprovar(PerfilUsuario.DIRETOR_FINANCEIRO, 1L);

        assertThat(proposta.getStatus())
                .isEqualTo(StatusContrato.ATIVO);

        assertThat(proposta.getParcelas())
                .hasSize(3);

        verify(repository).salvar(proposta);
    }
}