package br.ifsp.edu.scl.patrocinioesportivo.application.mutation;

import br.ifsp.edu.scl.patrocinioesportivo.model.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.AuthenticationInfoService;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.*;
import br.ifsp.edu.scl.patrocinioesportivo.service.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


import static br.ifsp.edu.scl.patrocinioesportivo.application.mutation.DadosMutacao.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void AprovacaoDeveRetornarOContratoAtivo() {
        var consultar = mock(ConsultarContratoService.class);
        var aprovar = mock(AprovarPropostaService.class);
        var auth = mock(AuthenticationInfoService.class);
        when(auth.getAuthenticatedUserProfile()).thenReturn(PerfilUsuario.DIRETOR_FINANCEIRO);
        when(consultar.consultar(7L)).thenReturn(contrato(StatusContrato.ATIVO));
        var controller = propostas(consultar, mock(EditarPropostaService.class), aprovar,
                mock(CancelarPropostaService.class), auth);

        var resposta = controller.aprovar(7L);

        assertThat(resposta).isNotNull();
        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.status()).isEqualTo(StatusContrato.ATIVO);
        assertThat(resposta.parcelas()).hasSize(1);
        assertThat(resposta.parcelas().getFirst().numero()).isEqualTo(7);
        verify(aprovar).aprovar(PerfilUsuario.DIRETOR_FINANCEIRO, 7L);
    }

    @Test
    void CancelamentoDeveRetornarAPropostaCancelada() {
        var consultar = mock(ConsultarContratoService.class);
        var cancelar = mock(CancelarPropostaService.class);
        when(consultar.consultar(7L)).thenReturn(contrato(StatusContrato.CANCELADO));
        var controller = propostas(consultar, mock(EditarPropostaService.class),
                mock(AprovarPropostaService.class), cancelar, mock(AuthenticationInfoService.class));

        var resposta = controller.cancelar(7L);

        assertThat(resposta).isNotNull();
        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.status()).isEqualTo(StatusContrato.CANCELADO);
        assertThat(resposta.parcelas()).isEmpty();
        verify(cancelar).cancelar(7L);
    }
}