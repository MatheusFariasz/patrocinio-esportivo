package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.OperacaoRedundanteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.service.CancelarPropostaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;


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

    @ParameterizedTest
    @EnumSource(
            value = StatusContrato.class,
            names = {"ATIVO", "EM_RISCO", "ENCERRADO", "RECUSADO"}
    )
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#3 e #4 - não deve cancelar proposta com status inválido")
    void naoDeveCancelarPropostaComStatusInvalido(StatusContrato status) {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(status);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(proposta));

        CancelarPropostaService service =
                new CancelarPropostaService(repository);

        assertThatThrownBy(() -> service.cancelar(1L))
                .isInstanceOf(TransicaoDeStatusInvalidaError.class);

        assertThat(proposta.getStatus())
                .isEqualTo(status);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#5 - deve lançar erro ao tentar cancelar proposta já cancelada")
    void deveLancarErroAoTentarCancelarPropostaJaCancelada() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(StatusContrato.CANCELADO);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(proposta));

        CancelarPropostaService service =
                new CancelarPropostaService(repository);

        assertThatThrownBy(() -> service.cancelar(1L))
                .isInstanceOf(OperacaoRedundanteError.class);

        assertThat(proposta.getStatus())
                .isEqualTo(StatusContrato.CANCELADO);
    }
}
